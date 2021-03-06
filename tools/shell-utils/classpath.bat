
set webappPath=civilizer
set extraPath=extra
set extraLibPath="!extraPath!\lib"
if not exist "!webappPath!\WEB-INF\web.xml" (
    if not exist "!extraLibPath!\jetty-runner.jar" (
        set webappPath="..\..\target\civilizer-1.0.0.CI-SNAPSHOT"
        set extraPath="..\..\target\extra"
        set extraLibPath="..\..\extra\lib"
        if not exist "!webappPath!\WEB-INF\web.xml" (
            if not exist "!extraLibPath!\jetty-runner.jar" (
                echo [ %hostScript% ][ ERROR ] Civilizer can't be found!
                exit /b 1
            )
        )        
    )
)

call :toAbsolutePath webappPath 
call :toAbsolutePath extraPath
call :toAbsolutePath extraLibPath

set classPath=!webappPath!\WEB-INF\classes;!extraLibPath!\*;!webappPath!\WEB-INF\lib\*;!extraPath!

if [%1] == [debug] (
    echo webappPath = !webappPath!
    echo extraPath = !extraPath!
    echo extraLibPath = !extraLibPath!
    echo classPath = !classPath!
    echo.
)

goto :eof

:toAbsolutePath
    call pushd "%%%1%%"
    set %1=%cd%
    popd
    exit /b 0
