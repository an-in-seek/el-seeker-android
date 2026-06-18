# AAB 파일 로컬 설치 및 Logcat 확보 가이드

Windows PC에서 ElSeeker 릴리즈 AAB를 실제 Android 기기에 설치하고, 앱 실행 중 발생하는 크래시 로그를 확보하는 절차이다. 서명된 AAB를 만드는 방법은 [release-signing-guide.md](release-signing-guide.md)를 먼저 확인한다.

## 1. 사전 준비

| 항목 | 값 |
|------|------|
| 패키지 이름 (`applicationId`) | `com.elseeker.android` |
| 테스트 폴더 | `C:\AAB_Test` |
| AAB 파일 예시 | `C:\AAB_Test\app-release.aab` |
| 키스토어 파일 예시 | `C:\AAB_Test\elseeker-release.jks` |
| bundletool 파일 예시 | `C:\AAB_Test\bundletool.jar` |

필요한 파일과 도구는 다음과 같다.

- 서명된 릴리즈 AAB: `app-release.aab`
- AAB를 테스트 APK 세트로 변환할 때 사용할 키스토어: 예: `elseeker-release.jks`
- `bundletool.jar`: [Google bundletool GitHub Releases](https://github.com/google/bundletool/releases)에서 다운로드
- Android SDK Platform Tools: Android Studio SDK Manager로 설치하거나 `%LOCALAPPDATA%\Android\Sdk\platform-tools` 사용
- 실제 Android 기기: 개발자 옵션의 **USB 디버깅** 활성화

> Play App Signing을 사용하는 앱은 Play Store가 최종 APK를 앱 서명 키로 다시 서명한다. 이 문서의 로컬 설치는 AAB 패키징, R8/ProGuard, 릴리즈 빌드 동작 확인용이며, Play 배포와 완전히 같은 서명 환경은 아니다. Play 배포와 동일한 조건까지 확인하려면 Play Console 내부 테스트 트랙을 사용한다.

## 2. PowerShell 작업 환경 준비

PowerShell을 열고 테스트 폴더로 이동한다.

```powershell
Set-Location C:\AAB_Test
```

`bundletool`이 `adb`를 찾을 수 있도록 현재 PowerShell 세션의 `PATH`에 Platform Tools를 추가한다. `adb.exe`, `AdbWinApi.dll`, `AdbWinUsbApi.dll`을 `C:\AAB_Test`에 복사한 경우에도 아래 명령을 그대로 사용해도 된다.

```powershell
$env:PATH = "C:\AAB_Test;${env:LOCALAPPDATA}\Android\Sdk\platform-tools;${env:PATH}"
```

Java 실행도 확인한다.

```powershell
java -version
```

기기가 정상 연결됐는지 확인한다.

```powershell
adb devices
```

출력에 기기 시리얼과 `device`가 보여야 한다. `unauthorized`가 보이면 휴대폰 잠금을 해제하고 USB 디버깅 허용 팝업을 승인한 뒤 다시 실행한다.

## 3. AAB를 APK 세트로 변환

아래 명령의 `--ks`, `--ks-key-alias`, 비밀번호 값을 실제 키스토어 정보로 바꾼다.

```powershell
java -jar .\bundletool.jar build-apks --bundle=.\app-release.aab --output=.\app.apks --overwrite --ks=.\elseeker-release.jks "--ks-pass=pass:키스토어비밀번호" --ks-key-alias=elseeker "--key-pass=pass:키비밀번호"
```

성공하면 `C:\AAB_Test\app.apks` 파일이 생성된다.

> 명령어 히스토리에 키스토어 비밀번호를 남기기 싫다면 `--ks-pass`를 생략하고 프롬프트에서 입력한다. 키 비밀번호가 키스토어 비밀번호와 같다면 `--key-pass`는 생략할 수 있다. 둘이 다르면 `file:C:\AAB_Test\key-pass.txt`처럼 비밀번호 파일을 지정하고, 작업 후 해당 파일을 삭제한다.

## 4. 기존 앱 제거

기기에 같은 패키지가 이미 설치되어 있으면 서명 키나 버전 차이 때문에 설치가 실패할 수 있으므로 먼저 제거한다.

```powershell
adb uninstall com.elseeker.android
```

`Success`가 나오면 정상 제거된 것이다. 이 명령은 앱 데이터도 삭제한다. 아직 설치된 앱이 없어서 실패 메시지가 나오는 경우는 무시해도 된다.

## 5. APK 세트 설치

`bundletool install-apks`로 현재 연결된 기기에 필요한 APK 조합을 설치한다.

```powershell
java -jar .\bundletool.jar install-apks --apks=.\app.apks
```

명령이 멈춘 것처럼 보이면 휴대폰 화면을 확인한다. USB 디버깅 허용, USB로 앱 설치 허용, 대용량 앱 설치 확인 같은 보안 팝업이 떠 있으면 승인해야 설치가 이어진다.

여러 기기나 에뮬레이터가 연결되어 있으면 `adb devices`에서 대상 시리얼을 확인한 뒤 `--device-id`를 지정한다.

```powershell
java -jar .\bundletool.jar install-apks --apks=.\app.apks --device-id=기기시리얼
```

## 6. 앱 실행

휴대폰에서 ElSeeker 앱을 직접 실행하거나, PowerShell에서 아래 명령으로 실행한다.

```powershell
adb shell am start -n com.elseeker.android/.ElSeekerActivity
```

## 7. Android Studio Logcat으로 로그 확보

1. Android Studio에서 **View > Tool Windows > Logcat**을 연다.
2. 상단의 대상 기기가 테스트 휴대폰인지 확인한다.
3. Logcat 검색창에 아래 필터를 입력한다.

```text
package:com.elseeker.android level:error
```

4. 앱을 실행하고 크래시를 재현한다.
5. `FATAL EXCEPTION`, `AndroidRuntime`, `Caused by`가 포함된 스택 트레이스를 복사한다.

로그가 너무 적거나 앱 시작 직후 프로세스가 종료되어 놓치는 경우에는 필터를 잠시 비우고 다시 재현한다.

## 8. PowerShell로 로그 파일 저장

Android Studio를 쓰기 어렵다면 PowerShell에서도 로그를 파일로 저장할 수 있다.

```powershell
adb logcat -c
adb logcat -v time | Tee-Object .\elseeker-logcat.txt
```

두 번째 명령을 실행한 상태에서 앱 크래시를 재현하고, 로그가 충분히 쌓이면 `Ctrl+C`로 종료한다. 결과 파일은 `C:\AAB_Test\elseeker-logcat.txt`에 저장된다.

## 9. 자주 발생하는 오류

| 오류/증상 | 조치 |
|-----------|------|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 기존 앱이 다른 서명 키로 설치된 상태이다. `adb uninstall com.elseeker.android` 후 다시 설치한다. |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | 기기에 더 높은 `versionCode` 앱이 설치된 상태이다. 기존 앱을 제거하거나 `versionCode`를 올린 AAB로 다시 빌드한다. |
| `more than one device/emulator` | `adb devices`로 시리얼을 확인하고 `adb -s 기기시리얼 ...` 또는 `install-apks --device-id=기기시리얼`을 사용한다. |
| `adb`를 찾을 수 없음 | PowerShell 세션에서 `$env:PATH` 설정을 다시 실행하거나 `%LOCALAPPDATA%\Android\Sdk\platform-tools` 설치 여부를 확인한다. |
| 설치 명령이 멈춤 | 휴대폰 화면의 USB 디버깅/USB 앱 설치 확인 팝업을 승인한다. |
| 소셜 로그인만 실패 | 로컬 설치에 사용한 서명 키의 해시가 Kakao/Naver/Google 콘솔에 등록되어 있는지 확인한다. |

## 참고

- Android Developers: [bundletool](https://developer.android.com/tools/bundletool)
- Android Developers: [Android Debug Bridge](https://developer.android.com/tools/adb)
- Android Developers: [View logs with Logcat](https://developer.android.com/studio/debug/logcat)
