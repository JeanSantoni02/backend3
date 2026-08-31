# 🏦 Migración de Procesos Batch - Banco XYZ

## 📌 Descripción del Proyecto
Proyecto de migración de procesos batch legacy del Banco XYZ utilizando **Spring Batch**. 
Moderniza tres procesos clave del sistema legacy, garantizando la integridad y consistencia de los datos.

## 🛠️ Tecnologías Utilizadas
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Batch 5.1.0**
- **PostgreSQL 18**
- **Maven**
- **Lombok**

## 📊 Procesos Implementados

### 1. Reporte de Transacciones Diarias (`jobReporteTransacciones`)
- Lee el archivo `transacciones.csv`
- Valida fechas, montos y tipos de transacción
- Marca anomalías (montos negativos, tipos inválidos, fechas incorrectas)
- Genera un resumen en consola

### 2. Cálculo de Intereses Mensuales (`jobIntereses`)
- Lee el archivo `intereses.csv`
- Valida datos de cuentas (saldo, edad, tipo)
- Prepara los datos para cálculo de intereses

### 3. Generación de Estados de Cuenta Anuales (`jobCuentaAnual`)
- Lee el archivo `cuentas_anuales.csv`
- Valida fechas, montos y descripciones
- Compila datos anuales para auditorías

## 📁 Estructura del Proyecto
