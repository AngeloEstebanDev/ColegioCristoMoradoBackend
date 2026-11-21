const SECRETO = 'CAMBIA-ESTO-por-una-clave-larga-inventada';

function doPost(e) {
  try {
    const d = JSON.parse(e.postData.contents);
    if (String(d.secreto || '').trim() !== SECRETO.trim()) return salida({ ok: false, error: 'no autorizado' });
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

function doGet() {
  return salida({ ok: true, servicio: 'relay Cristo Morado' });
}

function probar() {
  MailApp.sendEmail(Session.getActiveUser().getEmail(), 'Prueba Cristo Morado', 'Si te llegó, el script funciona.');
}

function salida(o) {
  return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);
}
