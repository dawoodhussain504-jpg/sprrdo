import { Request, Response, NextFunction } from 'express';
import { verifyToken, TokenPayload, UserRole } from '../config/jwt';

export interface AuthenticatedRequest extends Request {
  user?: TokenPayload;
}

export function authenticate(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ success: false, message: 'Authentication required. Missing Bearer token.' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const payload = verifyToken(token);
    req.user = payload;
    next();
  } catch (error: any) {
    return res.status(401).json({ success: false, message: 'Invalid or expired token', error: error.message });
  }
}

export function requireRole(allowedRoles: UserRole | UserRole[]) {
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];

  return (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    authenticate(req, res, () => {
      if (!req.user || !roles.includes(req.user.role)) {
        return res.status(403).json({
          success: false,
          message: `Forbidden. Role '${req.user?.role || 'unknown'}' does not have access to this resource. Allowed: [${roles.join(', ')}]`,
        });
      }
      next();
    });
  };
}
