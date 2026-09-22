/**
 * Controlador de impresión para tickets térmicos en formato 80mm / 88mm.
 */
window.printThermalTicket = function (ticketHtml) {
    if (!ticketHtml) {
        const elem = document.getElementById('printable-ticket-content');
        if (elem) {
            ticketHtml = elem.innerHTML;
        }
    }

    const iframe = document.createElement('iframe');
    iframe.id = 'thermal-ticket-print-frame';
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    document.body.appendChild(iframe);

    const doc = iframe.contentWindow.document;
    doc.open();
    doc.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Impresión de Ticket</title>
            <style>
                @page {
                    size: 88mm auto;
                    margin: 0mm;
                }
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                body {
                    width: 80mm;
                    max-width: 88mm;
                    margin: 0 auto;
                    padding: 4mm 3mm;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 11px;
                    color: #000;
                    background: #fff;
                    text-align: center;
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                }
                .ticket-title {
                    font-size: 13px;
                    font-weight: 700;
                    text-transform: uppercase;
                    margin-bottom: 2px;
                }
                .ticket-subtitle {
                    font-size: 10px;
                    margin-bottom: 4px;
                }
                .ticket-divider {
                    border-top: 1px dashed #000;
                    margin: 6px 0;
                }
                .ticket-row {
                    display: flex;
                    justify-content: space-between;
                    font-size: 11px;
                    margin: 3px 0;
                    text-align: left;
                }
                .ticket-row .lbl {
                    font-weight: 700;
                }
                .ticket-qr-container {
                    margin: 8px auto;
                    text-align: center;
                }
                .ticket-qr-container img {
                    width: 155px;
                    height: 155px;
                    display: block;
                    margin: 0 auto;
                }
                .ticket-uuid-code {
                    font-size: 9px;
                    font-family: monospace;
                    word-break: break-all;
                    margin-top: 3px;
                }
                .ticket-tarifa {
                    font-size: 10px;
                    margin: 4px 0;
                    line-height: 1.3;
                }
                .ticket-footer {
                    font-size: 9.5px;
                    margin-top: 6px;
                    line-height: 1.35;
                }
            </style>
        </head>
        <body>
            ${ticketHtml}
        </body>
        </html>
    `);
    doc.close();

    setTimeout(() => {
        iframe.contentWindow.focus();
        iframe.contentWindow.print();
        setTimeout(() => {
            if (document.body.contains(iframe)) {
                document.body.removeChild(iframe);
            }
        }, 1500);
    }, 250);
};
