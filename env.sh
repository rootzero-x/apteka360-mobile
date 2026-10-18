# Lokal toolchain (sudo'siz o'rnatilgan). Ishlatish: source env.sh
export JAVA_HOME="$HOME/.local/opt/jdk17"
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$HOME/.local/opt/node22/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
