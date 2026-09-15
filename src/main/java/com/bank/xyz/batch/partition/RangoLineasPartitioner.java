package com.bank.xyz.batch.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


public class RangoLineasPartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(RangoLineasPartitioner.class);

    public static final String CLAVE_INICIO = "lineasASaltar";
    public static final String CLAVE_CANTIDAD = "cantidadLineas";
    public static final String CLAVE_NOMBRE = "nombreParticion";

    private final Resource recurso;
    private final int lineasDeCabecera;

    public RangoLineasPartitioner(Resource recurso, int lineasDeCabecera) {
        this.recurso = recurso;
        this.lineasDeCabecera = lineasDeCabecera;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long totalFilas = contarFilasDeDatos();
        int particiones = Math.max(1, gridSize);

        if (totalFilas == 0) {
            log.warn("El archivo {} no tiene filas de datos.", recurso.getFilename());
            particiones = 1;
        }

        long base = totalFilas / particiones;
        long resto = totalFilas % particiones;

        Map<String, ExecutionContext> mapa = new LinkedHashMap<>();
        long cursor = lineasDeCabecera;

        for (int i = 0; i < particiones; i++) {
            long cantidad = base + (i < resto ? 1 : 0);
            String nombre = "particion-" + i;

            ExecutionContext contexto = new ExecutionContext();
            contexto.putLong(CLAVE_INICIO, cursor);
            contexto.putLong(CLAVE_CANTIDAD, cantidad);
            contexto.putString(CLAVE_NOMBRE, nombre);
            mapa.put(nombre, contexto);

            log.info("{} -> salta {} lineas y procesa {} filas de {}",
                    nombre, cursor, cantidad, recurso.getFilename());

            cursor += cantidad;
        }
        return mapa;
    }

    private long contarFilasDeDatos() {
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            long lineas = lector.lines().filter(linea -> !linea.isBlank()).count();
            return Math.max(0, lineas - lineasDeCabecera);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo leer el archivo para particionar: " + recurso.getFilename(), e);
        }
    }
}
