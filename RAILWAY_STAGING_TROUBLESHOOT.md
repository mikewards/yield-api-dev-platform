# Railway Staging Troubleshooting

## Issue
Staging deployment succeeded, but requests return:
```json
{"status":"error","code":404,"message":"Application not found","request_id":"..."}
```

This error format doesn't match our application's error format, suggesting Railway's routing layer is intercepting requests.

## Possible Causes

1. **Deployment Propagation Delay**: Railway may need a few minutes to fully propagate the new deployment
2. **Health Check Failure**: Railway might be marking the service as unhealthy
3. **Port Configuration**: Railway might not be detecting the correct port
4. **Service Not Fully Started**: The container might be running but not ready

## Solutions to Try

### 1. Check Service Status in Railway Dashboard
- Go to `flow-platform-staging` service
- Check **Metrics** tab → Look for CPU/Memory usage
- Check **Logs** tab → Verify app is running and listening on port 8080
- Check **Deployments** tab → Verify latest deployment is "Active"

### 2. Verify Health Check
Railway is configured with `healthcheckPath: "/health"` in `railway.json`. 
- Check if Railway can reach `/health` endpoint
- Look for health check failures in logs

### 3. Check Port Configuration
- Railway should auto-detect port 8080
- If not, you may need to set `PORT=8080` environment variable
- Or Railway might need `$PORT` environment variable

### 4. Wait and Retry
- Sometimes Railway needs 2-5 minutes to fully propagate
- Try again after a few minutes

### 5. Manual Restart
- In Railway dashboard → `flow-platform-staging` → **Deployments**
- Click **"Redeploy"** or **"Restart"**
- Wait for deployment to complete

### 6. Check Domain/Networking
- Verify **Public Networking** is enabled
- Check if domain is correctly generated
- Try accessing via the Railway-generated domain

### 7. Compare with Production
Production is working, so compare:
- Environment variables (should be different for staging)
- Database connection (should point to staging DB)
- Branch configuration (staging should be `staging` branch)

## Expected Behavior

Once working, you should see:
- `/health` returns: `{"status":"healthy","timestamp":"..."}`
- `/` returns: API info with service name
- `/v1/markets` returns market data (with auth)

## Next Steps

1. Wait 2-5 minutes and test again
2. Check Railway dashboard for any errors
3. Compare staging vs production configuration
4. If still not working, try manual restart

