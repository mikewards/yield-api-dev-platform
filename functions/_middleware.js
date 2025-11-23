// Cloudflare Pages Functions middleware
// Handles clean URLs by redirecting to .html files

export function onRequest(context) {
  const url = new URL(context.request.url);
  const pathname = url.pathname;
  
  // List of routes that should redirect to .html files
  const routes = {
    '/signin': '/signin.html',
    '/account': '/account.html',
    '/dashboard': '/dashboard.html'
  };
  
  // If this is a known route, redirect to the .html file
  if (routes[pathname]) {
    return Response.redirect(new URL(routes[pathname], url.origin), 301);
  }
  
  // Otherwise, continue with the request
  return context.next();
}

