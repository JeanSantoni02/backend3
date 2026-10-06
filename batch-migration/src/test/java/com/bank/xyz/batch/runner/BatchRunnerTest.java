package com.bank.xyz.batch.runner;

import com.bank.xyz.batch.config.BatchProperties;
import com.bank.xyz.batch.config.TransaccionesJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchRunnerTest {

    private JobLauncher launcher;
    private Job job;
    private BatchProperties propiedades;

    @BeforeEach
    void preparar() {
        launcher = mock(JobLauncher.class);
        job = mock(Job.class);
        when(job.getName()).thenReturn(TransaccionesJobConfig.JOB);

        propiedades = new BatchProperties();
        propiedades.setReejecucionesMaximas(2);
        propiedades.setEsperaReejecucionMs(1);
    }

    private JobExecution ejecucion(BatchStatus estado) {
        JobExecution e = new JobExecution(1L);
        e.setStatus(estado);
        return e;
    }

    @Test
    void unJobFallidoSeRelanzaConLosMismosParametrosYTerminaBien() throws Exception {
        when(launcher.run(eq(job), any()))
                .thenReturn(ejecucion(BatchStatus.FAILED))
                .thenReturn(ejecucion(BatchStatus.COMPLETED));

        BatchRunner runner = new BatchRunner(launcher, List.of(job), propiedades);
        runner.run(new DefaultApplicationArguments("--job=transacciones"));

        ArgumentCaptor<JobParameters> parametros = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher, times(2)).run(eq(job), parametros.capture());

        // Mismos parametros: Spring Batch lo trata como reinicio de la misma instancia
        assertThat(parametros.getAllValues().get(0)).isEqualTo(parametros.getAllValues().get(1));
        assertThat(runner.getExitCode()).isZero();
    }

    @Test
    void trasAgotarLasReejecucionesTerminaConError() throws Exception {
        when(launcher.run(eq(job), any())).thenReturn(ejecucion(BatchStatus.FAILED));

        BatchRunner runner = new BatchRunner(launcher, List.of(job), propiedades);
        runner.run(new DefaultApplicationArguments("--job=transacciones"));

        // El intento original mas las dos reejecuciones
        verify(launcher, times(3)).run(eq(job), any());
        assertThat(runner.getExitCode()).isEqualTo(1);
    }

    @Test
    void unJobQueTerminaBienNoSeRelanza() throws Exception {
        when(launcher.run(eq(job), any())).thenReturn(ejecucion(BatchStatus.COMPLETED));

        BatchRunner runner = new BatchRunner(launcher, List.of(job), propiedades);
        runner.run(new DefaultApplicationArguments("--job=transacciones"));

        verify(launcher, times(1)).run(eq(job), any());
        assertThat(runner.getExitCode()).isZero();
    }
}
