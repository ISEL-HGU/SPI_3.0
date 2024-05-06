import subprocess
import os

def compile_SimFix() -> bool:
    try:
        os.chdir("core/SimFix")
        command = "/home/jun4161/paths/jdk1.7.0_80/bin/javac \
            -d bin \
            -cp lib/com.gzoltar-0.1.1-jar-with-dependencies.jar:lib/commons-io-2.5.jar:lib/dom4j-2.0.0-RC1.jar:lib/json-simple-1.1.1.jar:lib/log4j-1.2.17.jar:lib/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar:lib/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar:lib/org.eclipse.core.resources_3.10.1.v20150725-1910.jar:lib/org.eclipse.core.runtime_3.11.1.v20150903-1804.jar:lib/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar:lib/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar:lib/org.eclipse.jdt.core_3.11.2.v20160128-0629.jar:lib/org.eclipse.osgi_3.10.102.v20160118-1700.jar:lib/org.eclipse.text-3.5.101.jar \
            $(find src -name '*.java')"
        subprocess.run(command, shell=True)
    except Exception as e:
        
        return False
    return True

def run_SimFix() -> bool:
    try:
        
        command = "/home/jun4161/paths/jdk1.7.0_80/bin/java \
            -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
            -classpath bin:lib/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar:lib/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar:lib/org.eclipse.core.resources_3.10.1.v20150725-1910.jar:lib/org.eclipse.core.runtime_3.11.1.v20150903-1804.jar:lib/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar:lib/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar:lib/org.eclipse.jdt.core_3.11.2.v20160128-0629.jar:lib/org.eclipse.osgi_3.10.102.v20160118-1700.jar:/Users/choejunhyeog/.p2/pool/plugins/org.junit_4.13.2.v20230809-1000.jar:/Users/choejunhyeog/.p2/pool/plugins/org.hamcrest_2.2.0.jar:/Users/choejunhyeog/.p2/pool/plugins/org.hamcrest.core_2.2.0.v20230809-1000.jar:lib/log4j-1.2.17.jar:lib/com.gzoltar-0.1.1-jar-with-dependencies.jar:lib/dom4j-2.0.0-RC1.jar:lib/commons-io-2.5.jar:lib/json-simple-1.1.1.jar \
            cofix.main.Main --proj_home=/home/jun4161/data2/APR/SPImFix/d4j_checkout \
            --proj_name=closure \
            --bug_id=10"

        subprocess.run(command, shell=True)
    except Exception as e:
        
        return False
    return True

if __name__ == "__main__":
    compile_SimFix()
    run_SimFix()