/**
 * Envío de códigos de verificación — I.E. 2092 Cristo Morado
 * Pega este código en https://script.google.com con el Gmail que enviará los correos.
 * 1. Cambia SECRETO por una clave larga inventada (la misma va en Render: MAIL_RELAY_SECRET).
 * 2. Implementar > Nueva implementación > Aplicación web
 *    Ejecutar como: Yo  ·  Quién tiene acceso: Cualquier usuario
 * 3. Copia la URL que termina en /exec y ponla en Render: MAIL_RELAY_URL.
 */
const SECRETO = 'CAMBIA-ESTO-por-una-clave-larga-inventada';

function doPost(e) {
  try {
    const d = JSON.parse(e.postData.contents);
    if (d.secreto !== SECRETO) return salida({ ok: false, error: 'no autorizado' });
    if (!d.para || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.para)) return salida({ ok: false, error: 'correo inválido' });
    MailApp.sendEmail({
      to: d.para,
      subject: String(d.asunto || 'Código de verificación').slice(0, 200),
      htmlBody: String(d.html || ''),
      name: d.nombre || 'I.E. 2092 Cristo Morado',
    });
    return salida({ ok: true });
  } catch (err) {
    return salida({ ok: false, error: String(err) });
  }
}

/** Ejecútala una vez desde el editor para dar permiso de enviar correos (y probar). */
function probar() {
  MailApp.sendEmail(Session.getActiveUser().getEmail(), 'Prueba Cristo Morado', 'Si te llegó, el script funciona.');
}

function salida(o) {
  return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);
}
