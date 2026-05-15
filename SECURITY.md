# Security policy

## Supported versions

PhotoSwipe is distributed only as source and as the latest debug APK attached to merged pull requests. Security updates land on the `main` branch and ship with the next merged PR. Older builds are not maintained.

## Reporting a vulnerability

If you believe you have found a security issue in PhotoSwipe, please **do not** open a public GitHub issue.

Instead, contact the maintainer privately by opening a private security advisory on GitHub:

1. Go to the [Security tab](https://github.com/Leonxlnx/image-sorter-app/security) of the repository.
2. Click "Report a vulnerability".
3. Include reproduction steps, the affected app version, and any logs or proof of concept you have.

We aim to acknowledge new reports within 7 days. Coordinated disclosure is appreciated — please give us a reasonable window to fix the issue before publishing details.

## Scope

In-scope examples:

- Bugs that cause unintended deletion of, or write to, user media outside the app's intended paths.
- Bypasses of the Android delete prompt for media the user did not author.
- Permission misuse or accidental exfiltration of media.

Out-of-scope examples:

- Vulnerabilities in third-party gallery apps that read the folders PhotoSwipe creates.
- Local attacks that require already-rooted devices.
- The user intentionally enabling a setting that has the documented effect.
