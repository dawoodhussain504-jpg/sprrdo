import fs from 'fs';
import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { getFileUrl } from '../middleware/upload';
import { db } from '../config/db';
import { notifyAdmins } from '../services/notification';

export async function uploadKycDocument(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const documentType = req.body.document_type; // 'vehicle_reg', 'aadhaar', 'selfie', 'payment_qr'

    if (!req.file) {
      return res.status(400).json({ success: false, message: 'No file uploaded' });
    }

    if (!documentType || !['vehicle_reg', 'aadhaar', 'selfie', 'payment_qr'].includes(documentType)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid document_type. Must be one of: vehicle_reg, aadhaar, selfie, payment_qr',
      });
    }

    const fileUrl = getFileUrl(req, req.file.filename);
    const docId = 'kyc_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);

    // Read uploaded file buffer and store base64 in database for indestructible persistence
    let fileBase64: string | null = null;
    const mimeType = req.file.mimetype || 'image/jpeg';
    try {
      if (req.file.path && fs.existsSync(req.file.path)) {
        fileBase64 = fs.readFileSync(req.file.path).toString('base64');
      } else if (req.file.buffer) {
        fileBase64 = req.file.buffer.toString('base64');
      }
    } catch (e: any) {
      console.warn('Failed reading file buffer for base64 storage:', e.message);
    }

    // Delete existing doc of this type for this captain if any
    await db.query('DELETE FROM kyc_documents WHERE captain_id = $1 AND document_type = $2', [captainId, documentType]);

    // Insert new document record with persistent file_data
    await db.query(
      `INSERT INTO kyc_documents (id, captain_id, document_type, file_url, file_data, mime_type, status, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
      [docId, captainId, documentType, fileUrl, fileBase64, mimeType]
    );

    // If it's payment_qr, update captain's payment_qr_url and payment_qr_data field directly
    if (documentType === 'payment_qr') {
      await db.query(
        'UPDATE captains SET payment_qr_url = $1, payment_qr_data = $2, payment_qr_mime = $3, updated_at = CURRENT_TIMESTAMP WHERE id = $4',
        [fileUrl, fileBase64, mimeType, captainId]
      );
    }

    // Check how many documents captain has uploaded
    const countRes = await db.query('SELECT COUNT(DISTINCT document_type) as count FROM kyc_documents WHERE captain_id = $1', [captainId]);
    const uploadedTypesCount = Number(countRes.rows[0]?.count || 0);

    let kycStatus = 'pending';
    if (uploadedTypesCount >= 4) {
      kycStatus = 'under_review';
      await db.query('UPDATE captains SET kyc_status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2', ['under_review', captainId]);

      // Notify admins that a captain has submitted complete KYC
      const captRes = await db.query('SELECT name FROM captains WHERE id = $1', [captainId]);
      const captName = captRes.rows[0]?.name || 'Captain';
      await notifyAdmins(
        'New KYC Submission Ready for Review',
        `${captName} has submitted all 4 KYC documents and is waiting for review.`,
        'kyc_submitted',
        { captainId }
      );
    }

    return res.status(201).json({
      success: true,
      message: `${documentType} uploaded successfully`,
      data: {
        document: {
          id: docId,
          captain_id: captainId,
          document_type: documentType,
          file_url: fileUrl,
          status: 'pending',
        },
        uploadedTypesCount,
        kycStatus,
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Upload failed', error: error.message });
  }
}

export async function getKycStatus(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;

    const captRes = await db.query('SELECT kyc_status, admin_remarks, payment_qr_url FROM captains WHERE id = $1', [captainId]);
    if (captRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Captain not found' });
    }

    const docsRes = await db.query('SELECT * FROM kyc_documents WHERE captain_id = $1 ORDER BY created_at DESC', [captainId]);

    const requiredDocs = ['vehicle_reg', 'aadhaar', 'selfie', 'payment_qr'];
    const uploadedMap: Record<string, any> = {};
    for (const doc of docsRes.rows) {
      uploadedMap[doc.document_type] = doc;
    }

    const documentStatuses = requiredDocs.map((type) => ({
      document_type: type,
      is_uploaded: Boolean(uploadedMap[type]),
      file_url: uploadedMap[type]?.file_url || null,
      status: uploadedMap[type]?.status || 'missing',
      admin_remarks: uploadedMap[type]?.admin_remarks || null,
    }));

    let paymentQrUrl = captRes.rows[0].payment_qr_url;
    if (!paymentQrUrl && uploadedMap['payment_qr']?.file_url) {
      paymentQrUrl = uploadedMap['payment_qr'].file_url;
      try {
        await db.query('UPDATE captains SET payment_qr_url = $1 WHERE id = $2', [paymentQrUrl, captainId]);
      } catch (_) {}
    }

    return res.json({
      success: true,
      data: {
        kyc_status: captRes.rows[0].kyc_status,
        admin_remarks: captRes.rows[0].admin_remarks,
        payment_qr_url: paymentQrUrl,
        documents: documentStatuses,
        is_complete: documentStatuses.every((d) => d.is_uploaded),
        is_approved: captRes.rows[0].kyc_status === 'approved',
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch KYC status', error: error.message });
  }
}
