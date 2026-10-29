// Configure NEO_MAIL_SECRET in Script Properties; never place it in source code.
function doPost(e) {
  try {
    const expected = PropertiesService.getScriptProperties().getProperty('NEO_MAIL_SECRET');
    const request = JSON.parse(e.postData.contents);
    if (!expected || expected.length < 32 || typeof request.secret !== 'string' ||
        request.secret !== expected || typeof request.to !== 'string' ||
        typeof request.subject !== 'string' || typeof request.html !== 'string' ||
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(request.to) ||
        request.subject.length > 200 || request.html.length > 20000) {
      return response_('ERROR');
    }
    MailApp.sendEmail({to: request.to, subject: request.subject, htmlBody: request.html, body: 'Abre este correo en un cliente compatible con HTML.'});
    return response_('SUCCESS');
  } catch (err) {
    return response_('ERROR');
  }
}

function response_(result) {
  return ContentService.createTextOutput(result).setMimeType(ContentService.MimeType.TEXT);
}
