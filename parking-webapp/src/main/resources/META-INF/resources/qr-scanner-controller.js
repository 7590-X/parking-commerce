window.QrScannerController = {
    instances: {},

    start: function (elementId, serverComponent) {
        this.stop(elementId);
        
        const container = document.getElementById(elementId);
        if (!container) {
            setTimeout(() => window.QrScannerController.start(elementId, serverComponent), 200);
            return;
        }

        if (typeof Html5Qrcode === "undefined") {
            console.error("Html5Qrcode is not loaded yet");
            if (serverComponent && serverComponent.$server) {
                serverComponent.$server.onScannerError("Librería de escáner no cargada aún.");
            }
            return;
        }

        try {
            const scanner = new Html5Qrcode(elementId);
            this.instances[elementId] = scanner;

            const config = {
                fps: 10,
                qrbox: (viewfinderWidth, viewfinderHeight) => {
                    const minEdgePercentage = 0.7;
                    const minEdgeSize = Math.min(viewfinderWidth, viewfinderHeight);
                    const qrboxSize = Math.floor(minEdgeSize * minEdgePercentage);
                    return {
                        width: Math.min(qrboxSize, 300),
                        height: Math.min(qrboxSize, 300)
                    };
                },
                aspectRatio: 1.0
            };

            scanner.start(
                { facingMode: "environment" },
                config,
                (decodedText) => {
                    if (serverComponent && serverComponent.$server) {
                        serverComponent.$server.onQrDecoded(decodedText);
                    }
                },
                (errorMessage) => {
                    // Frame scan failed, normal when searching for QR
                }
            ).catch(err => {
                console.warn("No se pudo iniciar la cámara trasera, intentando con cualquier cámara disponible:", err);
                // Fallback to default user camera
                scanner.start(
                    { facingMode: "user" },
                    config,
                    (decodedText) => {
                        if (serverComponent && serverComponent.$server) {
                            serverComponent.$server.onQrDecoded(decodedText);
                        }
                    },
                    (errorMsg) => {}
                ).catch(fallbackErr => {
                    console.error("Error al acceder a la cámara:", fallbackErr);
                    if (serverComponent && serverComponent.$server) {
                        serverComponent.$server.onScannerError("Cámara no disponible o permiso denegado: " + fallbackErr);
                    }
                });
            });
        } catch (e) {
            console.error("Error al inicializar Html5Qrcode:", e);
            if (serverComponent && serverComponent.$server) {
                serverComponent.$server.onScannerError("Excepción al inicializar escáner: " + e.message);
            }
        }
    },

    stop: function (elementId) {
        const scanner = this.instances[elementId];
        if (scanner) {
            try {
                if (scanner.isScanning) {
                    scanner.stop().then(() => {
                        try { scanner.clear(); } catch(e){}
                        delete window.QrScannerController.instances[elementId];
                    }).catch(() => {
                        delete window.QrScannerController.instances[elementId];
                    });
                    return;
                }
            } catch (e) {
                console.warn("Error deteniendo el escáner:", e);
            }
            delete this.instances[elementId];
        }
    }
};
