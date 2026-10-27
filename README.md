# SMS Forwarder

Copies the SMS you choose (for example MoMo and bank alerts) to your own email.
Built for Veraflow: the emails land in Gmail, and Veraflow reads them.

## What it does
- Listens for new SMS. Only senders you list are kept; everything else is ignored.
- Sends each one by email through your Gmail (SMTP, port 465).
- Keeps messages in a queue and retries when the phone is offline.
- The activity log shows the sender and the result, never the message text.

## Get the APK (no computer tools needed)
1. Create a new **private** repository on GitHub and upload everything in this folder. If the hidden `.github` folder does not upload, use **Add file, Create new file**, type `.github/workflows/build.yml` as the name and paste the contents of that file.
2. Open the **Actions** tab, choose **Build APK**, and press **Run workflow**.
3. When it finishes (about 5 to 10 minutes), open the run and download the **sms-forwarder-apk** file.
4. Unzip it, copy `app-release.apk` to your phone and open it. Android will ask you to allow installing from that source.

## Set up on the phone
1. Open the app and press **Allow** for SMS.
2. In Google Messages, note the exact sender name of your MoMo and bank alerts and type them in **Senders to forward**.
3. Gmail: turn on 2-step verification, then create an **app password** (Google Account, Security, App passwords). Paste it in the app.
4. Put your Gmail in both address boxes if you send to yourself. Press **Send test email**.
5. In Veraflow, Settings, Email alerts: add the Gmail address that sends these messages under **Addresses you forward from by hand**.
6. Android Settings, Apps, SMS Forwarder, Battery: choose **Unrestricted**.

## Good to know
- Only plain SMS is seen, not RCS chats. Bank and MoMo alerts are normally SMS.
- The app password is stored inside the app's private storage on the phone. Revoke it from your Google account at any time.
- Not on Google Play. Google restricts SMS permissions on the Play Store, so install the APK yourself.
- iPhone is not supported: iOS does not let apps read SMS.
- This code has not been compiled yet. If the build fails, send me the error from the Actions log.
