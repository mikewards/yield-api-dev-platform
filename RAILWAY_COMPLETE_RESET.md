# Railway Complete Reset - Delete Everything & Start Fresh

## Step 1: Delete Everything on Railway

### Delete API Service
1. Go to Railway dashboard
2. Click on `flow-platform` (or whatever your API service is named)
3. Click **Settings** tab
4. Scroll to bottom → Click **Delete Service**
5. Confirm deletion

### Delete Database Service
1. Go to Railway dashboard
2. Click on `flow-db` (or whatever your PostgreSQL service is named)
3. Click **Settings** tab
4. Scroll to bottom → Click **Delete Service**
5. Confirm deletion

**✅ Everything is now deleted. Clean slate!**

---

## Step 2: Create PostgreSQL Database (flow-db)

1. Railway dashboard → Click **New Project** (or use existing project)
2. Click **New** → **Database** → **PostgreSQL**
3. Name it: `flow-db`
4. Wait ~30 seconds for it to provision
5. **Don't touch anything** - Railway will auto-create variables

---

## Step 3: Get Database Connection Info

1. Click on `flow-db` service
2. Click **Variables** tab
3. **Copy these values** (don't edit anything):
   - `DATABASE_URL` → Copy entire value (starts with `postgresql://`)
   - `PGPASSWORD` → Copy the password value
   - Note: `POSTGRES_USER` is usually `postgres` (verify if needed)

**Keep these values handy - you'll need them in Step 5!**

---

## Step 4: Create API Service (flow-platform)

1. Railway dashboard → Click **New** → **GitHub Repo**
2. Select your repository: `wardmic4/flow-platform` (or your repo name)
3. Railway will auto-detect it's a Docker project
4. Name it: `flow-platform`
5. **Don't set any variables yet** - we'll do that next

---

## Step 5: Link Database to API

1. Click on `flow-platform` service
2. Click **Settings** tab
3. Scroll to **Service Dependencies**
4. Click **Add Service** → Select `flow-db`
5. Railway may auto-add `DATABASE_URL` - **we'll verify/override it in next step**

---

## Step 6: Set All Environment Variables

Go to `flow-platform` → **Variables** tab → Add these 5 variables:

### Variable 1: DATABASE_URL
- **Name:** `DATABASE_URL`
- **Value:** (paste the entire value from `flow-db` → Variables → `DATABASE_URL`)
- Should look like: `postgresql://postgres:password@flow-db.railway.internal:5432/railway`
- **Important:** Remove any leading/trailing spaces!

### Variable 2: DATABASE_USER
- **Name:** `DATABASE_USER`
- **Value:** `postgres`

### Variable 3: DATABASE_PASSWORD
- **Name:** `DATABASE_PASSWORD`
- **Value:** (paste from `flow-db` → Variables → `PGPASSWORD`)

### Variable 4: JWT_SECRET
- **Name:** `JWT_SECRET`
- **Value:** `d8635b0aab843c152ee6711673593f5b67c563b769c02fd04a5730577e503aa3`

### Variable 5: MASTER_ENCRYPTION_KEY
- **Name:** `MASTER_ENCRYPTION_KEY`
- **Value:** `e103e74a7c9f87e7b4b608a8c55114220f91960a7083df431152725cb0d1e734`

**✅ Save all variables**

---

## Step 7: Verify Setup

1. Railway will automatically redeploy `flow-platform`
2. Click on `flow-platform` → **Deployments** tab
3. Click on the latest deployment → **View Logs**
4. Look for:
   - ✅ `✅ Database tables created successfully`
   - ✅ `Application started`
   - ❌ If you see errors, check the error message

---

## Step 8: Test the API

1. Get your Railway URL: `flow-platform` → **Settings** → **Domains** → Copy the URL
2. Test health endpoint: `https://your-url.railway.app/health`
3. Should return: `OK`

---

## Checklist

- [ ] Deleted `flow-platform` service
- [ ] Deleted `flow-db` service
- [ ] Created new `flow-db` (PostgreSQL)
- [ ] Copied `DATABASE_URL` from `flow-db`
- [ ] Copied `PGPASSWORD` from `flow-db`
- [ ] Created new `flow-platform` service (from GitHub)
- [ ] Linked `flow-db` to `flow-platform`
- [ ] Set `DATABASE_URL` in `flow-platform`
- [ ] Set `DATABASE_USER` = `postgres` in `flow-platform`
- [ ] Set `DATABASE_PASSWORD` in `flow-platform`
- [ ] Set `JWT_SECRET` in `flow-platform`
- [ ] Set `MASTER_ENCRYPTION_KEY` in `flow-platform`
- [ ] Verified deployment logs show success
- [ ] Tested `/health` endpoint

---

## Common Issues

### "DATABASE_URL is missing"
- Make sure you set it in `flow-platform` (not `flow-db`)
- Copy the entire value from `flow-db` → Variables

### "Connection failed"
- Check `DATABASE_URL` has no leading/trailing spaces
- Verify `DATABASE_PASSWORD` matches `PGPASSWORD` from `flow-db`

### "Tables already exist"
- This is fine - means database is working!
- The app will continue running

---

## Need Help?

If something goes wrong, check:
1. Railway logs (most helpful)
2. All 5 variables are set correctly
3. No spaces in `DATABASE_URL`
4. Values copied exactly from `flow-db`

