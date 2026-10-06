// Capturas para el informe tecnico; requiere puppeteer-core y la plataforma arriba

import { mkdir } from 'node:fs/promises';
import puppeteer from 'puppeteer-core';

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const DESTINO = 'evidencias/09-eft/capturas';

const PANTALLAS = [
  ['01-eureka-registro', 'http://localhost:8761', 1440, 980, '#instances, table'],
  ['02-swagger-clientes', 'http://localhost:8085/swagger-ui.html', 1440, 900, '.opblock'],
  ['03-swagger-cuentas', 'http://localhost:8086/swagger-ui.html', 1440, 900, '.opblock'],
  ['04-swagger-pagos', 'http://localhost:8087/swagger-ui.html', 1440, 900, '.opblock'],
  ['05-alertas-seguridad', 'http://localhost:8084/api/v1/alertas', 1440, 520, 'body'],
  ['06-notificaciones', 'http://localhost:8084/api/v1/notificaciones?cantidad=6', 1440, 520, 'body'],
  ['07-circuito-cuentas', 'http://localhost:8086/actuator/circuitbreakers', 1440, 360, 'body'],
  ['08-gateway-respaldo', 'https://localhost:8443/respaldo/pagos', 1440, 300, 'body']
];

await mkdir(DESTINO, { recursive: true });

const navegador = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  acceptInsecureCerts: true,
  args: ['--ignore-certificate-errors']
});
const pagina = await navegador.newPage();

for (const [nombre, url, ancho, alto, selector] of PANTALLAS) {
  await pagina.setViewport({ width: ancho, height: alto });
  await pagina.goto(url, { waitUntil: 'networkidle0', timeout: 30000 });
  await pagina.waitForSelector(selector, { timeout: 15000 }).catch(() => {});

  // Las respuestas JSON se muestran con sangria para que se lean en el informe
  await pagina.evaluate(() => {
    const pre = document.querySelector('body > pre');
    if (pre) {
      try {
        pre.textContent = JSON.stringify(JSON.parse(pre.textContent), null, 2);
        pre.style.cssText = 'font: 13px/1.45 Consolas, monospace; padding: 14px; margin: 0;';
      } catch { /* no era JSON */ }
    }
  });

  await new Promise((r) => setTimeout(r, 900));
  await pagina.screenshot({ path: `${DESTINO}/${nombre}.png` });
  console.log('  ' + nombre + '.png');
}

await navegador.close();
