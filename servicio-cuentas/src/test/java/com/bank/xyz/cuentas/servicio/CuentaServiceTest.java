package com.bank.xyz.cuentas.servicio;

import com.bank.xyz.cuentas.dto.CuentaDto;
import com.bank.xyz.cuentas.error.ManejadorErrores.OperacionInvalidaException;
import com.bank.xyz.cuentas.evento.EventoTransaccion;
import com.bank.xyz.cuentas.evento.PublicadorEventos;
import com.bank.xyz.cuentas.modelo.Cuenta;
import com.bank.xyz.cuentas.remoto.ClientesRemoto;
import com.bank.xyz.cuentas.repositorio.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CuentaServiceTest {

    private CuentaRepository cuentas;
    private ClientesRemoto clientes;
    private PublicadorEventos publicador;
    private CuentaService servicio;

    @BeforeEach
    void preparar() {
        cuentas = mock(CuentaRepository.class);
        clientes = mock(ClientesRemoto.class);
        publicador = mock(PublicadorEventos.class);

        PlatformTransactionManager gestor = mock(PlatformTransactionManager.class);
        when(gestor.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        servicio = new CuentaService(cuentas, clientes, publicador, gestor);
        when(cuentas.save(any(Cuenta.class))).thenAnswer(i -> {
            Cuenta c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "cuentaId", 100000);
            return c;
        });
    }

    private Cuenta existente(int id, String saldo, Cuenta.EstadoCuenta estado) {
        Cuenta c = new Cuenta();
        ReflectionTestUtils.setField(c, "cuentaId", id);
        c.setSaldoFinal(new BigDecimal(saldo));
        c.setEstado(estado);
        when(cuentas.bloquear(id)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void abreLaCuentaConElNombreDelTitularYPublicaLaApertura() {
        when(clientes.obtener(1L)).thenReturn(
                new ClientesRemoto.ClienteResumen(1L, "12345678-5", "Diana Prince", "ACTIVO"));

        Cuenta c = servicio.abrir(new CuentaDto.Apertura(1L, "ahorro", new BigDecimal("25000")));

        assertThat(c.getNombre()).isEqualTo("Diana Prince");
        assertThat(c.getSaldoFinal()).isEqualByComparingTo("25000");
        assertThat(c.estadoEfectivo()).isEqualTo(Cuenta.EstadoCuenta.ACTIVA);
        verify(publicador).alConfirmar(any(EventoTransaccion.class));
    }

    @Test
    void noAbreCuentaAUnClienteInactivo() {
        when(clientes.obtener(2L)).thenReturn(
                new ClientesRemoto.ClienteResumen(2L, "11111111-1", "John Doe", "INACTIVO"));

        assertThatThrownBy(() -> servicio.abrir(new CuentaDto.Apertura(2L, "ahorro", null)))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("inactivo");
        verify(cuentas, never()).save(any());
    }

    @Test
    void noCierraUnaCuentaConSaldo() {
        existente(100001, "500", Cuenta.EstadoCuenta.ACTIVA);

        assertThatThrownBy(() -> servicio.cerrar(100001))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("saldo");
    }

    @Test
    void cierraUnaCuentaEnCero() {
        Cuenta c = existente(100001, "0", Cuenta.EstadoCuenta.ACTIVA);

        servicio.cerrar(100001);

        assertThat(c.estadoEfectivo()).isEqualTo(Cuenta.EstadoCuenta.CERRADA);
        assertThat(c.getCerradaEn()).isNotNull();
    }

    @Test
    void unaCuentaCerradaNoAdmiteMantenimiento() {
        existente(100002, "0", Cuenta.EstadoCuenta.CERRADA);

        assertThatThrownBy(() -> servicio.mantener(100002, new CuentaDto.Mantenimiento("corriente", null)))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unaCuentaMigradaSinEstadoSeConsideraActiva() {
        Cuenta c = new Cuenta();
        ReflectionTestUtils.setField(c, "cuentaId", 101);

        assertThat(c.estadoEfectivo()).isEqualTo(Cuenta.EstadoCuenta.ACTIVA);
        assertThat(c.esMigrada()).isTrue();
    }
}
