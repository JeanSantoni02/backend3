package com.bank.xyz.cuentas.servicio;

import com.bank.xyz.cuentas.dto.CuentaDto;
import com.bank.xyz.cuentas.error.ManejadorErrores.NoEncontradaException;
import com.bank.xyz.cuentas.error.ManejadorErrores.OperacionInvalidaException;
import com.bank.xyz.cuentas.evento.EventoTransaccion;
import com.bank.xyz.cuentas.evento.PublicadorEventos;
import com.bank.xyz.cuentas.modelo.Cuenta;
import com.bank.xyz.cuentas.modelo.Cuenta.EstadoCuenta;
import com.bank.xyz.cuentas.remoto.ClientesRemoto;
import com.bank.xyz.cuentas.repositorio.CuentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    private final CuentaRepository cuentas;
    private final ClientesRemoto clientes;
    private final PublicadorEventos publicador;
    private final TransactionTemplate transaccion;

    public CuentaService(CuentaRepository cuentas, ClientesRemoto clientes,
                         PublicadorEventos publicador, PlatformTransactionManager gestor) {
        this.cuentas = cuentas;
        this.clientes = clientes;
        this.publicador = publicador;
        this.transaccion = new TransactionTemplate(gestor);
    }

    @Transactional(readOnly = true)
    public Cuenta obtener(Integer id) {
        return cuentas.findById(id).orElseThrow(() -> new NoEncontradaException("no existe la cuenta " + id));
    }

    @Transactional(readOnly = true)
    public List<Cuenta> deCliente(Long clienteId) {
        return cuentas.findByClienteIdOrderByCuentaId(clienteId);
    }

    // Se valida al cliente fuera de la transaccion para no retener la conexion mientras responde
    public Cuenta abrir(CuentaDto.Apertura datos) {
        ClientesRemoto.ClienteResumen cliente = clientes.obtener(datos.clienteId());
        if (!cliente.activo()) {
            throw new OperacionInvalidaException("CLIENTE_INACTIVO",
                    "el cliente " + cliente.id() + " esta inactivo y no puede abrir cuentas");
        }
        return transaccion.execute(estado -> guardarApertura(cliente, datos));
    }

    private Cuenta guardarApertura(ClientesRemoto.ClienteResumen cliente, CuentaDto.Apertura datos) {
        BigDecimal deposito = datos.depositoInicial() == null ? BigDecimal.ZERO : datos.depositoInicial();

        Cuenta c = new Cuenta();
        c.setClienteId(cliente.id());
        c.setNombre(cliente.nombre());
        c.setTipo(datos.tipo());
        c.setSaldoFinal(deposito);
        c.setEstado(EstadoCuenta.ACTIVA);
        c.setAbiertaEn(LocalDateTime.now());
        c.setActualizadoEn(c.getAbiertaEn());

        Cuenta guardada = cuentas.save(c);
        log.info("Cuenta abierta | cuenta={} cliente={} tipo={} deposito={}",
                guardada.getCuentaId(), cliente.id(), datos.tipo(), deposito);

        publicador.alConfirmar(evento("APERTURA", guardada, deposito));
        return guardada;
    }

    @Transactional
    public Cuenta mantener(Integer id, CuentaDto.Mantenimiento datos) {
        Cuenta c = cuentas.bloquear(id).orElseThrow(() -> new NoEncontradaException("no existe la cuenta " + id));
        if (c.estadoEfectivo() == EstadoCuenta.CERRADA) {
            throw new OperacionInvalidaException("CUENTA_CERRADA", "una cuenta cerrada no admite cambios");
        }

        if (datos.tipo() != null) {
            c.setTipo(datos.tipo());
        }
        if (datos.estado() != null) {
            c.setEstado(EstadoCuenta.valueOf(datos.estado()));
        }
        c.setActualizadoEn(LocalDateTime.now());

        log.info("Mantenimiento de cuenta | cuenta={} tipo={} estado={}", id, c.getTipo(), c.estadoEfectivo());
        return c;
    }

    // Solo se cierra con saldo cero: el dinero tiene que salir antes por un retiro o transferencia
    @Transactional
    public Cuenta cerrar(Integer id) {
        Cuenta c = cuentas.bloquear(id).orElseThrow(() -> new NoEncontradaException("no existe la cuenta " + id));

        if (c.estadoEfectivo() == EstadoCuenta.CERRADA) {
            throw new OperacionInvalidaException("CUENTA_CERRADA", "la cuenta " + id + " ya esta cerrada");
        }
        BigDecimal saldo = c.getSaldoFinal() == null ? BigDecimal.ZERO : c.getSaldoFinal();
        if (saldo.signum() != 0) {
            throw new OperacionInvalidaException("SALDO_PENDIENTE",
                    "la cuenta tiene saldo " + saldo + "; debe quedar en cero antes del cierre");
        }

        c.setEstado(EstadoCuenta.CERRADA);
        c.setCerradaEn(LocalDateTime.now());
        c.setActualizadoEn(c.getCerradaEn());

        log.info("Cuenta cerrada | cuenta={}", id);
        publicador.alConfirmar(evento("CIERRE", c, BigDecimal.ZERO));
        return c;
    }

    private EventoTransaccion evento(String tipo, Cuenta c, BigDecimal monto) {
        String id = tipo + "-" + c.getCuentaId();
        return new EventoTransaccion(id, tipo, c.getCuentaId(), monto, c.getSaldoFinal(),
                id, "SUCURSAL", LocalDateTime.now());
    }
}
