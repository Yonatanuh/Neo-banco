import smtplib
import sys
import os
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

def send_email(to_email, subject, body_html):
    smtp_server = "smtp.gmail.com"
    smtp_port = 587
    sender_email = "jonathangran471@gmail.com"
    app_password = "psbbrdnxsavkkayv"

    msg = MIMEMultipart("alternative")
    msg['Subject'] = subject
    msg['From'] = f"Neo Banco <{sender_email}>"
    msg['To'] = to_email

    part = MIMEText(body_html, 'html')
    msg.attach(part)

    try:
        server = smtplib.SMTP(smtp_server, smtp_port, timeout=10)
        server.ehlo()
        server.starttls()
        server.ehlo()
        server.login(sender_email, app_password)
        server.sendmail(sender_email, to_email, msg.as_string())
        server.quit()
        print("SUCCESS")
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python send_email.py <to_email> <subject> <body_html_file>")
        sys.exit(1)
        
    to_email = sys.argv[1]
    subject = sys.argv[2]
    html_file = sys.argv[3]
    
    with open(html_file, 'r', encoding='utf-8', errors='replace') as f:
        body_html = f.read()
        
    send_email(to_email, subject, body_html)
    
    # Cleanup temp file
    try:
        os.remove(html_file)
    except:
        pass

