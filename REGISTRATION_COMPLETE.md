# Registration Page - Simple Authentication (No Firebase Required)

## ✅ Implementation Complete!

The registration page now accepts **any username and password** combination and proceeds directly to the recipes page.

## How It Works

### First Time Use:
1. User enters any username (doesn't need to be an email)
2. User enters any password (any length, any characters)
3. Click **"Next"** button
4. Credentials are saved locally using SharedPreferences
5. User is taken to the recipes page (MainActivity)

### Returning Users:
- When you reopen the app, it **remembers you're logged in**
- **Automatically skips** the registration page
- Goes directly to recipes page
- Shows "Welcome back, [username]!" message

## Features

✅ **No password requirements** - Any password works
✅ **No email validation** - Use any username format
✅ **Persistent login** - Stays logged in even after closing app
✅ **Clean UI** - No action bar, just the form
✅ **User feedback** - Toast messages for welcome and errors
✅ **No internet required** - Works completely offline
✅ **No Firebase setup needed** - Uses local storage only

## Technical Details

### Storage Method
- Uses Android **SharedPreferences** for local storage
- Saves username and login status
- Data persists across app sessions
- Stored in: `TasteBudsPrefs`

### Files Modified
1. `RegistrationActivity.kt` - Simplified authentication logic
2. `activity_registration.xml` - "Next" button text

### What Happens on "Next" Click
1. Validates username and password are not empty
2. Saves credentials to SharedPreferences
3. Navigates to MainActivity (recipes page)
4. Finishes RegistrationActivity (can't go back)

## Testing the App

### Test 1: First Login
1. Run the app
2. Enter username: `test` (or anything)
3. Enter password: `123` (or anything)
4. Click "Next"
5. ✅ Should see: "Welcome, test!" and recipes page

### Test 2: Remembered Login
1. Close the app completely
2. Reopen the app
3. ✅ Should automatically go to recipes page
4. ✅ Should see: "Welcome back, test!"

### Test 3: Empty Fields
1. Uninstall and reinstall app (to clear data)
2. Leave username or password empty
3. Click "Next"
4. ✅ Should see error: "Please enter both username and password"

## Need to Log Out?

To add a logout feature later (optional):
- Clear the SharedPreferences
- Navigate back to RegistrationActivity

Code snippet for logout:
```kotlin
val prefs = getSharedPreferences("TasteBudsPrefs", MODE_PRIVATE)
prefs.edit().clear().apply()
```

## Future Enhancements (Optional)

If you want to add Firebase authentication later:
- The structure is already in place
- Just uncomment Firebase code
- Add proper google-services.json
- Minimal code changes needed

