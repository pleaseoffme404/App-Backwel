import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname.startsWith('/admin')) {
    const sessionCookie = request.cookies.get('AUTH_SESSION_ID') || request.cookies.get('SESSION');

    if (!sessionCookie) {
      return NextResponse.redirect(new URL('http://localhost:8080/login'));
    }
  }

  if (pathname === '/admin') {
    return NextResponse.redirect(new URL('/admin/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/admin/:path*'],
};