# Signing and updates

For a durable personal installation, create one private keystore on your own computer:

```powershell
keytool -genkeypair -keystore repdawn-release.jks -alias repdawn -keyalg RSA -keysize 4096 -validity 10000
```

Keep that keystore and its passwords out of the repository and in two secure backups. Every later update must use the same key or Android will reject it. Add signing properties locally, configure the release signing block, then run `./gradlew.bat assembleRelease`. Never send passwords or a keystore through chat.
