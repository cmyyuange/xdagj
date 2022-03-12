#!/bin/sh

# =========================  function start  =========================

function buildJarPackage() {
    rootPath=$1
    randomXBuildPath="${rootPath}/src/c"
    randomXBuildFloderName="build"

    cd "${randomXBuildPath}"
    echo "xxxsr src root ${pwd}"
    echo "-e \033[44;37m ============  Build RandomX Lib Start  ============  \033[0m"
    sleep 4s

    # 判断文件是不是存在
    if [ ! -d "${randomXBuildFloderName}" ]; then
        mkdir "${randomXBuildFloderName}"
    fi
    cd ${randomXBuildFloderName}
    if [ $? -ne 0 ]; then
        echo "\033[31m Unable to create folder for RandomX script,The wrong path [${randomXBuildPath} ] please check \033[0m"
        exit 1
    fi
    cmake ..
    make
    if [ $? -ne 0 ]; then
        echo "\033[31m Build RandomX Lib failed  \033[0m"
        exit 1
    fi
    echo -e "\033[44;37m ============  Build RandomX Lib Finish & Success ============ \033[0m"
    echo -e "\033[44;37m ============  Build JAR Start  ============ \033[0m"
    sleep 3s

    cd ${rootPath}
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "\033[31m  Build JAR failed  \033[0m"
        exit 1
    fi
    echo -e "\033[44;37m ============  Build JAR Finish & Success  ============ \033[0m"

    sleep 3s
}

# $1 filename:  the file need to be copy
# $2 originalFilePath: the original path of file
# describe:  Determine whether there is file in the current directory, if it does not exist, copy it to the original path
function copyFile() {
    fileName=$1
    OriginalFilePath=$2

    # Check if the file exists in the current directory
    if [ ! -f "$fileName" ]; then
        echo "lack of [ $fileName ],  waiting for the copy"
        temp="${OriginalFilePath}/${fileName}"
        cp ${temp} ./
        # Whether the copy was successful
        if [ $? -ne 0 ]; then
            echo "\033[31m  copy ${fileName} failed   \033[0m"
        else
            echo "copy ${fileName} success"
        fi
    else
        echo "The [$fileName] are ready"
    fi

}

# =========================  function end  =========================

# =========================  constant start  =========================
# default JVM options
JAVA_OPTS="--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -server -Xms1g -Xmx1g"
XDAG_OPTS=""
XDAG_POOL_FOLDER_NAME="pool"
# find regular expressions for JAR packages
XDAG_JAR_REGEX="xdagj-*-shaded.jar"

# xdag config file name, default is mainnet
XDAG_TESTNET_CONFIG_NAME="xdag-testnet.config"
XDAG_MAINNET_CONFIG_NAME="xdag-mainnet.config"
XDAG_CONFIG_NAME=$XDAG_MAINNET_CONFIG_NAME

# optional startup parameters
USAGE="m"  # 表示主网
USAGE+="t" # 表示测试网
# =========================  constant end  =========================

# parses command line arguments
while getopts "$USAGE" opt; do
    case $opt in
    m)
        echo "prepare to start the main network pool"
        # add startup parameters
        XDAG_OPTS+="-m"
        XDAG_CONFIG_NAME=$XDAG_MAINNET_CONFIG_NAME
        ;;
    t)
        echo "prepare to start the test network pool"
        XDAG_OPTS+="-t"
        XDAG_CONFIG_NAME=$XDAG_TESTNET_CONFIG_NAME
        ;;
    ?)
        echo "unknown parameters $opt"
        exit 1
        ;;
    esac
done

# Make sure the path is the project root path, not the script folder path
tempPath=$(pwd)
rootPath=${tempPath%/script*}

# Get the name of the jar package available for execution in the target directory
targetPath="$rootPath/target/"
if [ ! -d "$targetPath" ]; then
    # If the target directory is missing, do package
    echo -e "\033[44;37m ============  首次启动构建  ============  \033[0m"
    buildJarPackage ${rootPath}
    echo -e "\033[44;37m ============  首次启动构建结束  ============  \033[0m"
fi
cd $targetPath
XDAG_JAR_NAME=$(ls ${XDAG_JAR_REGEX})

# the folder path where the mining pool data is located
XDAG_POOL_PATH="${rootPath}/${XDAG_POOL_FOLDER_NAME}"
# Check if the file exists, and create it if it doesn't
if [ ! -d "$XDAG_POOL_PATH" ]; then
    echo "create folder [ $XDAG_POOL_PATH ]"
    mkdir "$XDAG_POOL_PATH"
    if [ $? -ne 0 ]; then
        echo "create pool folder failed"
        exit 1
    else
        echo "create pool folder success"
    fi
else
    echo "folder [ $XDAG_POOL_PATH ] are ready"
fi

cd $XDAG_POOL_PATH
# copy config file & jar
copyFile $XDAG_JAR_NAME ${targetPath}
copyFile $XDAG_CONFIG_NAME "${rootPath}/src/main/resources"
copyFile "netdb-*.txt" "${rootPath}/src/main/resources"

# launch
echo -e "\033[44;37m ============  Mining Pool Is Starting......  ============  \033[0m"
java ${JAVA_OPTS} -cp .:${XDAG_JAR_NAME} io.xdag.Bootstrap "${XDAG_OPTS}"
