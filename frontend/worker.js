// Cloudflare Worker to handle clean URLs
// Redirects /signin, /account, /dashboard to their .html files

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const pathname = url.pathname;
    
    // Routes that should redirect to .html files
    const routes = {
      '/signin': '/signin.html',
      '/account': '/account.html',
      '/dashboard': '/dashboard.html',
      '/dashboard-team': '/dashboard-team.html',
      '/dashboard-webhooks': '/dashboard-webhooks.html',
      '/dashboard-logs': '/dashboard-logs.html',
      '/dashboard-settings': '/dashboard-settings.html'
    };
    
    // If this is a known route, redirect to the .html file
    if (routes[pathname]) {
      return Response.redirect(new URL(routes[pathname], url.origin), 301);
    }
    
    // Otherwise, let the assets handler serve the file
    return env.ASSETS.fetch(request);
  }
};

