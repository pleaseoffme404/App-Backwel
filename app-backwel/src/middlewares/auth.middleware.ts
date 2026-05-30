import { Request, Response, NextFunction } from 'express';

export const requireAuth = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
  const cookieHeader = req.headers.cookie;

  if (!cookieHeader) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }

  try {
    const gatewayUrl = process.env.GATEWAY_URL || 'http://localhost:8080';
    const authCheck = await fetch(`${gatewayUrl}/api/v1/user/userinfo`, {
      headers: { cookie: cookieHeader }
    });

    if (!authCheck.ok) {
      res.status(401).json({ error: 'unauthorized' });
      return;
    }

    next();
  } catch (error) {
    res.status(500).json({ error: 'internal_error' });
  }
};