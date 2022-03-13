#!/bin/sh

# =========================  constant start  =========================
# hint
XDAG_INFO="\033[34m [INFO] \033[0m"
XDAG_WARNING="\033[33m [WARNING] \033[0m"
XDAG_ERROR="\033[31m [ERROR] \033[0m"

# default JVM options
JAVA_OPTS="--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -server -Xms1g -Xmx1g"

# xdag launch options
XDAG_OPTS=""
XDAG_POOL_FOLDER_NAME="pool"
# find regular expressions for JAR packages
XDAG_JAR_REGEX="xdagj-*-shaded.jar"

# xdag config file name, default is mainnet
XDAG_TESTNET_CONFIG_NAME="xdag-testnet.config"
XDAG_MAINNET_CONFIG_NAME="xdag-mainnet.config"
XDAG_CONFIG_NAME=$XDAG_MAINNET_CONFIG_NAME

# optional startup parameters for getopts
USAGE="m"  # use mainnet
USAGE+="t" # use testnet
USAGE+="h" # help Print usage
# =========================  constant end  =========================

# =========================  function start  =========================

# Find the jar in the target directory
function getXdagJarName() {
    tempPath=$(pwd)
    # Make sure the path is the project root path, not the script folder path
    rootPath=${tempPath%/script*}

    # Get the name of the jar package available for execution in the target directory
    targetPath="$rootPath/target/"
    if [ ! -d "$targetPath" ]; then
        # If the target directory is missing, do package
        echo "\033[47;30m============  Build XDAGJ Project Start============\033[0m"
        buildXdagProject ${rootPath}
        echo "\033[47;30m============  Build XDAGJ Project Finish ============\033[0m"
    fi
    cd $targetPath
    XDAG_JAR_NAME=$(ls ${XDAG_JAR_REGEX})
    if [ -z XDAG_JAR_NAME ]; then
        echo "${XDAG_ERROR}The jar is missing in the target directory and cannot be run, please check"
        exit 1
    fi
}

function buildXdagProject() {
    rootPath=$1
    randomXBuildPath="${rootPath}/src/c"
    randomXBuildFloderName="build"

    cd "${randomXBuildPath}"
    echo "\033[47;30m============  Build RandomX Lib Start  ============\033[0m"

    # does /src/c/build exist
    if [ ! -d "${randomXBuildFloderName}" ]; then
        mkdir "${randomXBuildFloderName}"
    fi
    cd ${randomXBuildFloderName}
    if [ $? -ne 0 ]; then
        # Failed to create build file
        echo "${XDAG_ERROR}Unable to create folder for RandomX script,The wrong path [${randomXBuildPath} ] please check"
        exit 1
    fi
    cmake ..
    make
    if [ $? -ne 0 ]; then
        echo "${XDAG_ERROR}Build RandomX Lib failed"
        exit 1
    fi
    echo "\033[47;30m============  Build RandomX Lib Finish & Success ============\033[0m"
    echo ""
    echo "\033[47;30m============  Build JAR Start  ============\033[0m"

    cd ${rootPath}
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "${XDAG_ERROR}Build JAR failed"
        exit 1
    fi
    echo "\033[47;30m ============  Build JAR Finish & Success  ============ \033[0m"
}

# $1 filename:  the file need to be copy
# $2 originalFilePath: the original path of file
# describe:  Determine whether there is file in the current directory, if it does not exist, copy it to the original path
function copyFile() {
    fileName=$1
    OriginalFilePath=$2

    # Check if the file exists in the current directory
    if [ ! -f "$fileName" ]; then
        echo "${XDAG_INFO}lack of [ $fileName ],  waiting for the copy"
        temp="${OriginalFilePath}/${fileName}"
        cp ${temp} ./
        # Whether the copy was successful
        if [ $? -ne 0 ]; then
            echo "${XDAG_ERROR}copy ${fileName} failed"
        else
            echo "${XDAG_INFO}copy ${fileName} success"
        fi
    else
        echo "${XDAG_INFO}The file [$fileName] are ready"
    fi

}

# =========================  function end  =========================

# parses command line arguments
while getopts "$USAGE" opt; do
    case $opt in
    m)
        echo "${XDAG_INFO}Prepare to start the main network pool"
        # add startup parameters
        XDAG_OPTS+="-m"
        XDAG_CONFIG_NAME=$XDAG_MAINNET_CONFIG_NAME
        ;;
    t)
        echo "${XDAG_INFO}Prepare to start the test network pool"
        XDAG_OPTS+="-t"
        XDAG_CONFIG_NAME=$XDAG_TESTNET_CONFIG_NAME
        ;;
    h)
        echo "usage: sh xdag.sh [options]"
        echo "options:"
        echo "\\t-t: use testnet"
        echo "\\t-m: use maintest"
        echo "\\t-h: print help"
        exit 0
        ;;
    ?)
        echo "${XDAG_ERROR}unknown parameters $opt"
        exit 1
        ;;
    esac
done

# If you are using the release folder, there should already be a corresponding jar package and configuration file in the current directory
if (test ! -z "$(ls ${XDAG_JAR_REGEX} >/dev/null 2>&1)") && (test ! -z "$(ls ${XDAG_CONFIG_NAME} >/dev/null 2>&1)"); then
    XDAG_JAR_NAME="$(ls ${XDAG_JAR_REGEX})"
    echo "${XDAG_INFO}The file [${XDAG_JAR_NAME}] are ready"
    echo "${XDAG_INFO}The file [${XDAG_CONFIG_NAME}] are ready"
    echo "\033[42;37m============  Mining Pool Is Starting......  ============\033[0m"
    java ${JAVA_OPTS} -cp .:$(ls ${XDAG_JAR_NAME}) io.xdag.Bootstrap "${XDAG_OPTS}"
    exit 0
fi

# Get the name of the jar package available for execution in the target directory
getXdagJarName

# the folder path where the mining pool data is located
XDAG_POOL_PATH="${rootPath}/${XDAG_POOL_FOLDER_NAME}"
# Check if the file exists, and create it if it doesn't
if [ ! -d "$XDAG_POOL_PATH" ]; then
    echo "${XDAG_INFO}create folder [ $XDAG_POOL_PATH ]"
    mkdir "$XDAG_POOL_PATH"
    if [ $? -ne 0 ]; then
        echo "${XDAG_ERROR}create pool folder failed"
        exit 1
    else
        echo "${XDAG_INFO}create pool folder success"
    fi
else
    echo "${XDAG_INFO}folder [ $XDAG_POOL_PATH ] are ready"
fi

cd $XDAG_POOL_PATH
# copy config file & jar
copyFile $XDAG_JAR_NAME ${targetPath}
copyFile $XDAG_CONFIG_NAME "${rootPath}/src/main/resources"
copyFile "netdb-*.txt" "${rootPath}/src/main/resources"

# launch
echo "\033[42;37m============  Mining Pool Is Starting......  ============\033[0m"
java ${JAVA_OPTS} -cp .:${XDAG_JAR_NAME} io.xdag.Bootstrap "${XDAG_OPTS}"
