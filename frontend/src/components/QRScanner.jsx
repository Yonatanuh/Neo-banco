import { useEffect, useRef } from "react";
import { Html5QrcodeScanner } from "html5-qrcode";

const QRScanner = ({ onScanSuccess }) => {
  const scannerRef = useRef(null);

  useEffect(() => {
    // Only initialize once
    if (!scannerRef.current) {
      scannerRef.current = new Html5QrcodeScanner(
        "qr-reader",
        { fps: 10, qrbox: { width: 250, height: 250 } },
        false,
      );

      scannerRef.current.render(
        (decodedText) => {
          scannerRef.current.clear();
          onScanSuccess(decodedText);
        },
        (error) => {
          // console.warn(error);
        },
      );
    }

    return () => {
      if (scannerRef.current) {
        scannerRef.current.clear().catch(console.error);
      }
    };
  }, [onScanSuccess]);

  return (
    <div
      id="qr-reader"
      className="w-full overflow-hidden rounded-xl border border-white/20 bg-black/40"
    ></div>
  );
};

export default QRScanner;
