import shutil
import sys
import os
import platform
import zipfile
import subprocess
import traceback

import argparse
import configparser
import jproperties
import datetime as dt
import csv
import xml.etree.ElementTree as ET


def parse_argv() -> tuple:
    parser = argparse.ArgumentParser()

    parser.add_argument("-c", "--config",   type = str,     default = "SPI.ini",
                        help = "Tells on what environment to run SPI.")

    parser.add_argument("-d", "--debug",    action = "store_true")

    parser.add_argument("-r", "--rebuild",  action = "store_true",
                        help = "Rebuilds all SPI submodules if enabled.")
    
    parser.add_argument("-t","--textsim", action = "store_true", help = "On and Off options to use text sim or not")

    parser.add_argument("-A", "--APR",   type = str,     default = "ConFix",
                        help = "Tells on what APR do you want to use.")
    
    parser.add_argument("-p", "--pool", type = str, default = None, help="Use Predefined Patches")
    
    # parser.add_argument("-q", "--quiet",    action = 'store_true',
    #                     help = "Quiet output. Suppresses INFO messages, but errors and warnings will still be printed out.")
    # parser.add_argument("-v", "--verbose",  action = 'store_true',
    #                     help = "Detailed output. Use it to debug.")

    args = parser.parse_args()

    settings = configparser.ConfigParser()
    settings.optionxform = str
    settings.read(args.config)

    settings['SPI']['debug'] = str(args.debug)
    settings['SPI']['APR'] = str(args.APR)

    cases = list()
    if args.debug == True:
        cases.append(dict())
        settings['SPI']['mode'] = "defects4j" # override mode
        settings['SPI']['project'] = "Closure"
        cases[-1]['project_name'] = "Closure-14"
        cases[-1]['identifier'], cases[-1]['version'] = cases[-1]['project_name'].split("-")
        cases[-1]['iteration'] = 0
        cases[-1]['is_ConFix_ready'] = False
    else:
        if settings['SPI']['mode'] == "defects4j":
            cases.append(dict())
            cases[-1]['identifier'] = settings['SPI']['identifier']
            cases[-1]['version'] = settings['SPI']['version']
            cases[-1]['project_name'] = f"{cases[-1]['identifier']}-{cases[-1]['version']}"
            cases[-1]['iteration'] = 0
            cases[-1]['is_ConFix_ready'] = False

        elif settings['SPI']['mode'] in ("defects4j-batch", "defects4j-batch-expr"):
            print(f"debug : mode defects4j-batch")
            with open(settings['SPI']['batch_d4j_file'], "r") as infile:
                for bug in infile.read().splitlines():
                    cases.append(dict())
                    cases[-1]['project_name'] = bug
                    cases[-1]['identifier'], cases[-1]['version'] = bug.split("-")
                    cases[-1]['iteration'] = 0
                    cases[-1]['is_ConFix_ready'] = False
                
        elif settings['SPI']['mode'] == 'vjbench':
            cases.append(dict())
            cases[-1]['identifier'] = settings['SPI']['identifier']
            cases[-1]['version'] = settings['SPI']['version']
            cases[-1]['project_name'] = f"{cases[-1]['identifier']}-{cases[-1]['version']}"
            cases[-1]['iteration'] = 0
            cases[-1]['is_ConFix_ready'] = False

        elif settings['SPI']['mode'] == "github":
            pass
            # Enhance reading options here
            cases.append(dict())
            cases[-1]['repository'] = settings['SPI']['repository_url']
            cases[-1]['project_name'] = cases[-1]['repository'].rsplit("/", 1)[-1]
            cases[-1]['identifier'] = cases[-1]['project_name']
            cases[-1]['version'] = "0"
            cases[-1]['iteration'] = 0
            cases[-1]['is_ConFix_ready'] = False

    # settings['verbose'] = args.verbose
    # settings['quiet'] = False if args.verbose else args.quiet # suppresses quiet option if verbose option is given
    settings['SPI']['rebuild'] = str(args.rebuild)
    
    if args.pool is not None:
        settings['SPI']['with_predefined_pool'] = str(args.pool)
    else:
        settings['SPI']['with_predefined_pool'] = "False"
    
    if args.textsim == True:
        settings['LCE']['text_sim'] = "true"
    else:
        settings['LCE']['text_sim'] = "false"


    return (cases, settings)

#######
# Misc
#######

def copy(file, destination):
    try:
        shutil.copy(file, destination)
    except Exception as e:
        print(f"| SPI  | ! Error: {file} : {e.strerror}")
        print(f"| SPI  | ! Error occurred while copying {file} to {destination}.")
        return False
    else:
        return True

def move(location, destination, copy_function) -> bool:
    try:
        shutil.move(location, destination, copy_function)
    except Exception as e:
        print(f"| SPI  | ! Error: {location} : {e.strerror}")
        print(f"| SPI  | ! Error occurred while moving {location} to {destination}.")
        return False
    else:
        return True

def remove(path):
    try:
        if(os.path.exists(path)):
            if(os.path.isdir(path)):
                shutil.rmtree(path)
            else:
                os.remove(path)
    except Exception as e:
        print(f"| SPI  | ! Error: {path} : {e.strerror}")
        print(f"| SPI  | ! Error occurred while removing {path}.")
        return False
    else:
        return True

def unzip(file, destination):
    try:
        with zipfile.ZipFile(file, "r") as zip_ref:
            zip_ref.extractall(destination)
    except Exception as e:
        print(f"| SPI  | ! Error: {file} : {e.strerror}")
        print(f"| SPI  | ! Error occurred while unzipping {file}.")
        return False
    else:
        return True

#######
# Module rebuilder
#######

def rebuild(module_name : str, SPI_root) -> bool:
    try:
        subprocess.run(("gradle", "distZip", "-q"), cwd = os.path.join(SPI_root, "core", module_name), check = True)
        if not move(os.path.join(SPI_root, "core", module_name, "app", "build", "distributions", "app.zip"), os.path.join(SPI_root, "pkg"), copy_function = shutil.copy):
            raise RuntimeError("Failed to move distZip result to pkg/.")
        if not unzip(os.path.join(SPI_root, "pkg", "app.zip"), os.path.join(SPI_root, "pkg")):
            raise RuntimeError("Failed to unzip app.zip in pkg/.")
        if not os.path.exists(os.path.join(SPI_root, "pkg", module_name)):
            subprocess.run(("mkdir", module_name), cwd = os.path.join(SPI_root, "pkg"), check = True)
        if not move(os.path.join(SPI_root, "pkg", "app"), os.path.join(SPI_root, "pkg", module_name), shutil.copy2):
            raise RuntimeError(f"Failed to move app/ to {module_name}/.")

        os.chmod(os.path.join(SPI_root, "pkg", module_name, "app", "bin", "app"), 0o774)
        os.chmod(os.path.join(SPI_root, "pkg", module_name, "app", "bin", "app.bat"), 0o774)

        if not remove(os.path.join(SPI_root, "pkg", "app.zip")):
            raise RuntimeError("Failed to remove app.zip.")

    except Exception as e:
        print(f"| SPI  | ! Error occurred while rebuilding submodule {module_name}.")
        traceback.print_exc()
        return False
    else:
        print(f"| SPI  | Successfully rebuilt submodule {module_name}.")
        return True

# def rebuild_confix(SPI_root : str, JDK8_HOME : str) -> bool:
#     try:
#         subprocess.run(("mvn", "clean", "package", "-q"), cwd = os.path.join(SPI_root, "core", "confix", "ConFix-code"), check = True)
#         if not copy(os.path.join(SPI_root, "core", "confix", "ConFix-code", "target", "confix-0.0.1-SNAPSHOT-jar-with-dependencies.jar"), os.path.join(SPI_root, "core", "confix", "lib", "confix-ami_torun.jar")):
#             raise RuntimeError("Failed to copy ConFix.jar.")

#     except Exception as e:
#         print("| SPI  | ! Error occurred while rebuilding ConFix.")
#         traceback.print_exc()
#         return False
#     else:
#         return True

def rebuild_all(SPI_root : str, JDK8_HOME : str):
    try:
        if not (remove(os.path.join(SPI_root, "pkg"))):
            raise RuntimeError("Failed to remove directory pkg/.")
        subprocess.run(("mkdir", "pkg"), cwd = SPI_root, check = True)

        for submodule in ("ChangeCollector", "LCE"):
            log4j_xml_tree = ET.parse(os.path.join(SPI_root, "core", submodule, "app", "src", "main", "resources", 'log4j2.xml'))
            log4j_xml_root = log4j_xml_tree.getroot()
            for child in log4j_xml_root.find('Properties'):
                if child.get('name') == 'projectRoot':
                    child.text = SPI_root

            log4j_xml_tree.write(os.path.join(SPI_root, "core", submodule, "app", "src", "main", "resources", 'log4j2.xml'))

            rebuild(submodule, SPI_root)
        # # ConFix is rebuilt every launch. No need to rebuild it here.
        # rebuild_confix(SPI_root, JDK8_HOME) # ConFix uses maven unlike any other packages; this should be handled differently.
        # print(f"| SPI  | Successfully rebuilt submodule ConFix.")

    except Exception as e:
        print("| SPI  | ! Error occurred while rebuilding modules.")
        traceback.print_exc()
        return False
    else:
        return True


#######
# Module launcher
#######
    
def run_Pool_LCE(case : dict, is_defects4j : bool, conf_SPI : configparser.SectionProxy, conf_LCE : configparser.SectionProxy, path) -> bool:
    try:
        
        prop_LCE = jproperties.Properties()
        
        prop_LCE['pool.dir'] = os.path.join(case['target_dir'], "outputs", "LCE", "result")
        prop_LCE['candidates.dir'] = os.path.join(case['target_dir'], "outputs", "LCE", "candidates")
        
        os.makedirs(prop_LCE['pool.dir'].data)
        os.makedirs(prop_LCE['candidates.dir'].data)
        
        for file in os.listdir(path):
            print(file)
            shutil.copy(os.path.join(path, file), os.path.join(case['target_dir'], "outputs", "LCE", "candidates"))
        
    except Exception as e:
        traceback.print_exc()
        return False
    else:
        return True

def run_CC(case : dict, is_defects4j : bool, is_vjbench : bool, conf_SPI : configparser.SectionProxy, conf_CC : configparser.SectionProxy) -> bool:
    try:
        # copy .properties file
        prop_CC = jproperties.Properties()
        for key in conf_CC.keys():
            prop_CC[key] = conf_CC[key]
        prop_CC['project_root'] = conf_SPI['root']
        prop_CC['output_dir'] = conf_SPI['byproduct_path']
        prop_CC['JAVA_HOME.8'] = conf_SPI['JAVA_HOME_8']
        prop_CC['repository_path'] = conf_SPI['repository_path']

       
        # Explicitly tell 'target'
        prop_CC['mode'] = conf_SPI['mode']
    
        if conf_SPI['mode'] == 'defects4j-batch':
            prop_CC['mode'] = 'defects4j'
        
        prop_CC['hash_id'] = case['hash_id']
        if is_defects4j == True or is_vjbench == True:
            prop_CC['benchmark_name'] = case['identifier']
            prop_CC['benchmark_id'] = case['version']
        else:
            if prop_CC['mode'] == "file":
                pass

            prop_CC['git_url'] = conf_SPI['repository_url']
            prop_CC['git_name'] = conf_SPI['identifier']
            prop_CC['file_name'] = conf_SPI['faulty_file']
            prop_CC['commit_id'] = conf_SPI['commit_id']
            prop_CC['lineFix'] = conf_SPI['faulty_line_fix']
            prop_CC['lineBlame'] = conf_SPI['faulty_line_blame']

        with open(os.path.join(case['target_dir'], "properties", "CC.properties"), "wb") as f:
            prop_CC.store(f, encoding = "UTF-8")


        # run CC
        launch_command = ".\\app.bat" if platform.system() == "Windows" else "./app"
        with open(os.path.join(case['target_dir'], "logs", "CC.log"), "w") as f:
            subprocess.run([launch_command, os.path.join(case['target_dir'], "properties", "CC.properties")], cwd = os.path.join(conf_SPI['root'], "pkg", "ChangeCollector", "app", "bin"), stdout = f, check = True)
    except Exception as e:
        traceback.print_exc()
        return False
    else:
        return True

def run_LCE(case : dict, is_defects4j : bool, conf_SPI : configparser.SectionProxy, conf_LCE : configparser.SectionProxy) -> bool:
    try:
        prop_LCE = jproperties.Properties()
        for key in conf_LCE.keys():
            prop_LCE[key] = conf_LCE[key]

        prop_LCE['SPI.dir'] = conf_SPI['root']

        prop_LCE['pool_file.dir'] = os.path.join(conf_SPI['root'], "components", "LCE", "gumtree_vector.csv")
        prop_LCE['meta_pool_file.dir'] = os.path.join(conf_SPI['root'], "components", "LCE", "commit_file.csv")

        prop_LCE['target_vector.dir'] = os.path.join(case['target_dir'], "outputs", "ChangeCollector", f"{case['identifier']}_gumtree_vector.csv")

        prop_LCE['pool.dir'] = conf_SPI['stored_pool_dir']
        prop_LCE['textSimPool.dir'] = os.path.join(case['target_dir'], "outputs", "LCE", "result")
        prop_LCE['candidates.dir'] = os.path.join(case['target_dir'], "outputs", "LCE", "candidates")

        prop_LCE['d4j_project_name'] = case['identifier']
        prop_LCE['d4j_project_num'] = case['version']
        
        with open(os.path.join(case['target_dir'], "properties", "LCE.properties"), "wb") as f:
            prop_LCE.store(f, encoding = "UTF-8")

        os.makedirs(prop_LCE['candidates.dir'].data)

        launch_command = ".\\app.bat" if platform.system() == "Windows" else "./app"
        with open(os.path.join(case['target_dir'], "logs", "LCE.log"), "w") as f:
            subprocess.run([launch_command, os.path.join(case['target_dir'], "properties", "LCE.properties")], cwd = os.path.join(conf_SPI['root'], "pkg", "LCE", "app", "bin"), stdout = f, check = True)
    except Exception as e:
        traceback.print_exc()
        return False
    else:
        return True

def run_ConFix(case : dict, is_defects4j : bool, is_vjbench : bool, conf_SPI : configparser.SectionProxy, conf_ConFix : configparser.SectionProxy) -> bool:
    try:
        conf_runner = configparser.ConfigParser()
        conf_runner.optionxform = str
        conf_runner.add_section('Project')
        conf_runner['Project'] = conf_SPI
        with open(os.path.join(case['target_dir'], "properties", "confix_runner.ini"), "w") as f:
            conf_runner.write(f)


        prop_ConFix = jproperties.Properties()
        for key in conf_ConFix.keys():
            prop_ConFix[key] = conf_ConFix[key]

        prop_ConFix['jvm'] = os.path.join(conf_SPI['JAVA_HOME_8'], "bin", "java")
        prop_ConFix['version'] = "1.8"
        # prop_ConFix['version'] = "1.8" if conf_ConFix['version'] == "" else conf_ConFix['version'] # Commented out since only Java 8 seems to work.
        # prop_ConFix['pool.path'] = f"{os.path.join(conf_SPI['root'], 'core', 'confix', 'pool', 'ptlrh')},{os.path.join(conf_SPI['root'], 'core', 'confix', 'pool', 'plrt')}"
        prop_ConFix['pool.path'] = f"{os.path.join(conf_SPI['root'], 'Correct_patch')}" #SC: Reproducing purpose
        prop_ConFix['cp.lib'] = os.path.join(conf_SPI['root'], 'core', 'confix', 'lib', 'confix-ami_torun.jar')

        with open(os.path.join(case['target_dir'], "properties", "confix.properties"), "wb") as f:
            prop_ConFix.store(f, encoding = "UTF-8") 

        if is_defects4j == True:
            subprocess.run(["python3.6", os.path.join(conf_SPI['root'], "core", "confix", "run_confix.py"), "-d", "true", "-h", case['hash_id'], "-f", os.path.join(case['target_dir'], "properties", "confix_runner.ini")], check = True)
        elif is_vjbench == True:
            subprocess.run(["python3.6", os.path.join(conf_SPI['root'], "core", "confix", "run_confix.py"), "-v", "true", "-h", case['hash_id'], "-f", os.path.join(case['target_dir'], "properties", "confix_runner.ini")], check = True)
        else:
            subprocess.run(["python3.6", os.path.join(conf_SPI['root'], "core", "confix", "run_confix.py"), "-h", case['hash_id'], "-f", os.path.join(case['target_dir'], "properties", "confix_runner.ini")], check = True)
        # with open(os.path.join(case['target_dir'], "logs", "ConFix_runner.log"), "w") as f:
        #     if is_defects4j == True:
        #         subprocess.run(["python3", os.path.join(conf_SPI['root'], "core", "confix", "run_confix.py"), "-d", "true", "-h", case['hash_id'], "-f", os.path.join(case['target_dir'], "properties", "confix_runner.ini")], env = jdk8_env, stdout = f, check = True)
        #     else:
        #         subprocess.run(["python3", os.path.join(conf_SPI['root'], "core", "confix", "run_confix.py"), "-h", case['hash_id'], "-f", os.path.join(case['target_dir'], "properties", "confix_runner.ini")], env = jdk8_env, stdout = f, check = True)

    except Exception as e:
        traceback.print_exc()
        return False
    else:
        return True

def compile_SimFix(JAVA_Home_7: str) -> bool:
    try:
        command = JAVA_Home_7 +  "/bin/javac \
            -d bin \
            -cp lib/com.gzoltar-0.1.1-jar-with-dependencies.jar:lib/commons-io-2.5.jar:lib/dom4j-2.0.0-RC1.jar:lib/json-simple-1.1.1.jar:lib/log4j-1.2.17.jar:lib/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar:lib/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar:lib/org.eclipse.core.resources_3.10.1.v20150725-1910.jar:lib/org.eclipse.core.runtime_3.11.1.v20150903-1804.jar:lib/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar:lib/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar:lib/org.eclipse.jdt.core_3.11.2.v20160128-0629.jar:lib/org.eclipse.osgi_3.10.102.v20160118-1700.jar:lib/org.eclipse.text-3.5.101.jar \
            $(find src -name '*.java')"
        subprocess.run(command, shell=True)

    except Exception as e:
        traceback.print_exc()
        return False
    return True

def run_SimFix(JAVA_HOME_7: str, JAVA_HOME_8: str, d4j_checkout_dir: str, projectName: str, bugId: str, LCE_candidates_path: str) -> bool:
    try:
        command = JAVA_HOME_7 + "/bin/java \
            -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
            -classpath bin:lib/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar:lib/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar:lib/org.eclipse.core.resources_3.10.1.v20150725-1910.jar:lib/org.eclipse.core.runtime_3.11.1.v20150903-1804.jar:lib/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar:lib/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar:lib/org.eclipse.jdt.core_3.11.2.v20160128-0629.jar:lib/org.eclipse.osgi_3.10.102.v20160118-1700.jar:/Users/choejunhyeog/.p2/pool/plugins/org.junit_4.13.2.v20230809-1000.jar:/Users/choejunhyeog/.p2/pool/plugins/org.hamcrest_2.2.0.jar:/Users/choejunhyeog/.p2/pool/plugins/org.hamcrest.core_2.2.0.v20230809-1000.jar:lib/log4j-1.2.17.jar:lib/com.gzoltar-0.1.1-jar-with-dependencies.jar:lib/dom4j-2.0.0-RC1.jar:lib/commons-io-2.5.jar:lib/json-simple-1.1.1.jar \
            cofix.main.Main \
            --proj_home=" + d4j_checkout_dir + \
            " --proj_name=" +  projectName +  \
            " --bug_id=" + bugId + \
            " --LCE_candidates_path=" + LCE_candidates_path + \
            " --JAVA_HOME_8=" + JAVA_HOME_8

        subprocess.run(command, shell=True)
    except Exception as e:
        traceback.print_exc()
        return False
    return True

#######
# Main
#######

def main(argv):
    cases, settings = parse_argv()

    settings['SPI']['root'] = os.getcwd() if settings['SPI']['root'] == "" else settings['SPI']['root']
    settings['SPI']['byproduct_path'] = os.path.join(settings['SPI']['root'], "byproducts") if settings['SPI']['byproduct_path'] == "" else settings['SPI']['byproduct_path']

    print(f"| SPI  | SPI from directory {settings['SPI']['root']} is going to be launched.")
    print(f"| SPI  | SPI byproducts will be made in directory {settings['SPI']['byproduct_path']}.")

    
    is_defects4j = settings['SPI']['mode'] in ("defects4j", "defects4j-batch")
    is_vjbench = settings['SPI']['mode'] == 'vjbench'
    is_rebuild_required = (settings['SPI']['rebuild'] == "True")


    patch_strategies = ("flfreq", ) if (settings['SPI']['patch_strategy'] == "" or settings['SPI']['debug'] == "True") else tuple([each.strip() for each in settings['SPI']['patch_strategy'].split(',')])
    concretization_strategies = ("hash-match", ) if (settings['SPI']['concretization_strategy'] == "" or settings['SPI']['debug'] == "True") else tuple([each.strip() for each in settings['SPI']['concretization_strategy'].split(',')])
    print(f"| SPI  | SPI launching with patch strategies {patch_strategies}.")
    print(f"| SPI  | SPI launching with concretization strategies {concretization_strategies}.")

    if is_rebuild_required:
        print("| SPI  | Have been requested to rebuild all submodules. Commencing...")
        if rebuild_all(settings['SPI']['root'], settings['SPI']['JAVA_HOME_8']):
            print("| SPI  | All submodules have been successfully rebuilt.")
        else:
            print("| SPI  | ! Some of the submodules have failed to build, thus cannot execute SPI. Aborting the program.")
            sys.exit(-1)

    if settings['SPI']['mode'] is None:
        print("| SPI  | ! You have not told me what to fix. Exiting the program.")
        sys.exit(0)

    
    time_hash = str(abs(hash(f"{dt.datetime.now().strftime('%Y%m%d%H%M%S')}")))[-6:]
    SPI_launch_result_str = str()
    log_file = str()

    patch_abb = {"flfreq" : "ff", "tested-first" : "tf", "noctx" : "nc", "patch" : "pt"}
    concretization_abb = {"tcvfl" : "tv", "hash-match" : "hm", "neighbor" : "nb", "tc" : "tc"}

    # if settings['SPI']['APR'] == "SimFix":
    #     os.chdir('core/SimFix')
    #     identifierLower = settings['SPI']['identifier'].lower()
    #     bugId = settings['SPI']['version']
    #     if settings['SPI']['rebuild']:
    #         if not check_directory_existence("bin"):
    #             os.mkdir("bin")
    #         if not compile_SimFix(settings['SPI']['JAVA_HOME_7']):
    #             raise RuntimeError("Module 'ConFix' launch failed.")    
            
    #     if not check_directory_existence(settings['SimFix']['d4j_checkout_dir']):
    #         os.mkdir(settings['SimFix']['d4j_checkout_dir'])
    #     if not check_directory_existence(settings['SimFix']['d4j_checkout_dir'] + '/' + identifierLower):
    #         os.mkdir(settings['SimFix']['d4j_checkout_dir'] + '/' + identifierLower)
    #     if not check_directory_existence(settings['SimFix']['d4j_checkout_dir'] + '/' + identifierLower + '/' +identifierLower + '_' + bugId + '_buggy'):
    #         print("=======> ")
    #         checkout_command = "defects4j checkout -p {} -v {}b -w /tmp/{}_{}_buggy"
    #         mv_command = "mv /tmp/{}_{}_buggy {}/{}/{}_{}_buggy"
    #         os.system(checkout_command.format(settings['SPI']['identifier'], bugId, identifierLower, bugId))
    #         os.system(mv_command.format(identifierLower, bugId, settings['SimFix']['d4j_checkout_dir'], identifierLower, identifierLower, bugId))
    #         # print(checkout_command.format(settings['SPI']['identifier'], bugId, identifierLower, bugId))
    #         # print(mv_command.format(identifierLower, bugId, settings['SimFix']['d4j_checkout_dir'], identifierLower, identifierLower, bugId))
    #     if not run_SimFix(settings['SPI']['JAVA_HOME_7'], settings['SimFix']['d4j_checkout_dir'], identifierLower, bugId):
    #         raise RuntimeError("Module 'ConFix' launch failed.")
    #     return
        

    for patch_strategy in patch_strategies:
        for concretization_strategy in concretization_strategies:
            settings['ConFix']['patch.strategy'] = patch_strategy
            settings['ConFix']['concretize.strategy'] = concretization_strategy
            strategy_combination = f"{patch_strategy} + {concretization_strategy}"
            print(f"\n| SPI  | Strategy combination '{strategy_combination}' set.")

            # Initializations before whole case-loop
            failed = list()
            succeeded = list()


            # hash_prefix = f"batch_{time_hash}_{patch_abb[patch_strategy]}+{concretization_abb[concretization_strategy]}" if "batch" in settings['SPI']['mode'] else f"{time_hash}"
            hash_prefix = f"batch_{time_hash}" if "batch" in settings['SPI']['mode'] else f"{time_hash}"
            log_file = f"log_{hash_prefix}.txt"
            
            ###

            whole_start = dt.datetime.now()

            if not os.path.exists(os.path.join(settings['SPI']['root'], "logs")):
                os.makedirs(os.path.join(settings['SPI']['root'], "logs"))

            if not os.path.isfile(os.path.join(settings['SPI']['root'], "logs", log_file)):
                with open(os.path.join(settings['SPI']['root'], "logs", log_file), "x") as _:
                    pass # only make log file if it doesn't exist.
            
            with open(os.path.join(settings['SPI']['root'], "logs", log_file), "a") as outfile:
                outfile.write(f"Batch execution w/ Strategy '{strategy_combination}' commmenced in {whole_start}\n")

            for case_num, case in enumerate(cases, 1):
                ##########
                # Pre-launch configuration
                ##########

                case['hash_id'] = f"{hash_prefix}_{case['project_name']}"
                case['target_dir'] = os.path.join(settings['SPI']['byproduct_path'], case['hash_id'])
                case['iteration'] += 1
                cursor_str = f"Iteration #{case['iteration']} w/ Strategy '{strategy_combination} / Case #{case_num}"

                print(f"| SPI  | {cursor_str} | Begins to look for patch for {case['project_name']}...")

                # path preparation
                if case['iteration'] == 1:
                    os.makedirs(case['target_dir'])
                    os.makedirs(os.path.join(case['target_dir'], "logs"))
                    os.makedirs(os.path.join(case['target_dir'], "outputs"))
                    os.makedirs(os.path.join(case['target_dir'], "properties"))

                    print(f"| SPI  |    > {cursor_str} | Hash ID generated as {case['hash_id']}")
                    print(f"| SPI  |    > {cursor_str} | byproducts made in directory {case['target_dir']}.")

                """
                Search in root/components/commit_collector/Defects4J_bugs_info/<project>.csv for the version
                Then extract information of the bug: file, fix line, blame line
                """
                if is_defects4j == True:
                    settings['SPI']['identifier'] = case['identifier']
                    settings['SPI']['version'] = case['version']
                    with open(os.path.join(settings['SPI']['root'], "components", "commit_collector", "Defects4J_bugs_info", f"{case['identifier']}.csv"), "r", newline = "") as d4j_meta_file:
                        reader = csv.DictReader(d4j_meta_file)
                        for row in reader:
                            if int(row['Defects4J ID']) == int(case['version']):
                                settings['SPI']['faulty_file'] = row['Faulty file path']
                                settings['SPI']['faulty_line_fix'] = row['fix faulty line']
                                settings['SPI']['faulty_line_blame'] = row['blame faulty line']
                                break
                elif is_vjbench:
                    settings['SPI']['identifier'] = case['identifier']
                    settings['SPI']['version'] = case['version']
                    with open(os.path.join(settings['SPI']['root'], "components", "commit_collector", "VJBench_bugs_info", f"{case['identifier']}.csv"), "r", newline = "") as d4j_meta_file:
                        reader = csv.DictReader(d4j_meta_file)
                        for row in reader:
                            if int(row['VJBench ID']) == int(case['version']):
                                settings['SPI']['faulty_file'] = row['Faulty file path']
                                settings['SPI']['faulty_line_fix'] = row['fix faulty line']
                                settings['SPI']['faulty_line_blame'] = row['blame faulty line']
                                break
                else: # GitHub not implemented fully.
                    # print("| SPI  | ! SPI currently works on defects4j bugs only. Cannot launch those on other projects. Aborting program.")
                    # sys.exit(0)
                    settings['SPI']['identifier'] = case['identifier']

                ##########
                # Modules launch
                ##########

                each_start = dt.datetime.now()
                with open(os.path.join(settings['SPI']['root'], "logs", log_file), "a") as outfile:
                    outfile.write(f"    - Launching SPI upon {cursor_str}...\n")
                    outfile.write(f"       > Started at {each_start.strftime('%Y-%m-%d %H:%M:%S')}.\n")

                try:
                    if case['iteration'] == 1 or case['is_ConFix_ready'] == False: #execute CC and LCE
                    
                        if settings['SPI']['with_predefined_pool'] == "False":

                            print(f"| SPI  |    > {cursor_str} | Step 1. Running Change Collector...")
                            if not run_CC(case, is_defects4j, is_vjbench, settings['SPI'], settings['CC']):
                                raise RuntimeError("Module 'Change Collector' launch failed.")

                            print(f"| SPI  |    > {cursor_str} | Step 2. Running Longest Common subvector Extractor...")
                            if not run_LCE(case, is_defects4j, settings['SPI'], settings['LCE']):
                                raise RuntimeError("Module 'Longest Common subvector Extractor' launch failed.")
                        else:
                            if not run_Pool_LCE(case, is_defects4j, settings['SPI'], settings['LCE'], settings['SPI']['with_predefined_pool']): #because it has one more directory
                                raise RuntimeError("Module 'Longest Common subvector Extractor' launch failed.")
                  
                        case['is_ConFix_ready'] = True # Mark it True if those two CC and LCE has succeede in launch.
                    else:
                        print(f"| SPI  |    > {cursor_str} | Step 1 and Step 2 skipped.")

                    print(f"| SPI  |    > {cursor_str} / Step 3. Running SimFix...")
                    if settings['SPI']['APR'] == "SimFix":
                        os.chdir('core/SimFix')
                        identifierLower = settings['SPI']['identifier'].lower()
                        bugId = settings['SPI']['version']
                        if settings['SPI']['rebuild']:
                            if not check_directory_existence("bin"):
                                os.mkdir("bin")
                            if not compile_SimFix(settings['SPI']['JAVA_HOME_7']):
                                raise RuntimeError("Module 'ConFix' launch failed.")    
                            
                        if not check_directory_existence(settings['SimFix']['d4j_checkout_dir']):
                            os.mkdir(settings['SimFix']['d4j_checkout_dir'])
                        if not check_directory_existence(settings['SimFix']['d4j_checkout_dir'] + '/' + identifierLower):
                            os.mkdir(settings['SimFix']['d4j_checkout_dir'] + '/' + identifierLower)
                        if not check_directory_existence(settings['SimFix']['d4j_checkout_dir'] + '/' + identifierLower + '/' +identifierLower + '_' + bugId + '_buggy'):
                            
                            checkout_command = "defects4j checkout -p {} -v {}b -w /tmp/{}_{}_buggy"
                            mv_command = "mv /tmp/{}_{}_buggy {}/{}/{}_{}_buggy"
                            os.system(checkout_command.format(settings['SPI']['identifier'], bugId, identifierLower, bugId))
                            os.system(mv_command.format(identifierLower, bugId, settings['SimFix']['d4j_checkout_dir'], identifierLower, identifierLower, bugId))
                            # print(checkout_command.format(settings['SPI']['identifier'], bugId, identifierLower, bugId))
                            # print(mv_command.format(identifierLower, bugId, settings['SimFix']['d4j_checkout_dir'], identifierLower, identifierLower, bugId))

                        LCE_candidates_path = case['target_dir'] + '/outputs/LCE/candidates'
                        if not run_SimFix(settings['SPI']['JAVA_HOME_7'], settings['SPI']['JAVA_HOME_8'], settings['SimFix']['d4j_checkout_dir'], identifierLower, bugId, LCE_candidates_path):
                            raise RuntimeError("Module 'ConFix' launch failed.")
                        return
                    
                    print(f"| SPI  |    > {cursor_str} / Step 3. Running ConFix...")
                    if not run_ConFix(case, is_defects4j, is_vjbench, settings['SPI'], settings['ConFix']):
                        raise RuntimeError("Module 'ConFix' launch failed.")


                    # Check for patch existence
                    if os.path.isfile(os.path.join(case['target_dir'], "diff_file.txt")):
                        print(f"\n| SPI  | !  > {cursor_str} | [candidate metadata] > ConFix patch generation success")
                        print(f"| SPI  |    > {cursor_str} | Finished, and found a patch!")
                        print(f"| SPI  |    > {cursor_str} | === diff_file.txt starts ===")
                        with open(os.path.join(case['target_dir'], "diff_file.txt"), "r") as f:
                            content = f.read()
                            print(content)
                        print(f"| SPI  |    > {cursor_str} | === diff_file.txt ends ===")

                        if not copy(os.path.join(case['target_dir'], "diff_file.txt"), os.path.join(case['target_dir'], f"diff_file-{patch_abb[patch_strategy]}{concretization_abb[concretization_strategy]}.txt")):
                            raise RuntimeError("Failed to copy diff_file.txt.")
                        if not remove(os.path.join(case['target_dir'], "diff_file.txt")):
                            raise RuntimeError("Failed to remove diff_file.txt.")

                        succeeded.append(case['project_name'])
                        SPI_launch_result_str = "succeeded"
                    else:
                        print(f"| SPI  | !  > {cursor_str} | [candidate metadata] > ConFix patch generation fail")
                        print(f"\n| SPI  |    > {cursor_str} | Finished, but failed to find a patch.")

                        failed.append(case['project_name'])
                        SPI_launch_result_str = "failed"

                    if not copy(os.path.join(case['target_dir'], case['identifier'],  "log.txt"), os.path.join(case['target_dir'], "logs", f"ConFix-{patch_abb[patch_strategy]}{concretization_abb[concretization_strategy]}.txt")):
                        raise RuntimeError("Failed to copy log.txt.")
                    
                    # Make not to remove Defects4j Project
                    # TODO: Need to eliminate on the release
                    
                    #if not remove(os.path.join(case['target_dir'], case['identifier'])):
                        #raise RuntimeError("Failed to remove workspace folder.")

                    
                except Exception as e:
                    print()
                    print(f"| SPI  | !  > {cursor_str} | [candidate metadata] > ConFix patch generation fail")
                    print(f"| SPI  | !  > {cursor_str} | Aborted during progresses!")
                    print(f"| SPI  | !  > {cursor_str} | Failed cause: {e}")
                    traceback.print_exc()

                    failed.append(case['project_name'])
                    SPI_launch_result_str = "failure"
                finally:
                    each_end = dt.datetime.now()
                    each_elapsed_time = (each_end - each_start)

                    print(f"| SPI  |    > {cursor_str} | Elapsed Time : {each_elapsed_time}")

                    with open(os.path.join("logs", log_file), "a") as outfile:
                        outfile.write(f"       > Ended at {each_end.strftime('%Y-%m-%d %H:%M:%S')}.\n")
                        outfile.write(f"       > Elapsed time: {each_elapsed_time}.\n")
                        outfile.write(f"       > Patch generation: {SPI_launch_result_str}\n")


            whole_end = dt.datetime.now()
            whole_elapsed_time = (whole_end - whole_start)

            if "batch" in settings['SPI']['mode']:
                with open(os.path.join("logs", log_file), "a") as outfile:
                    outfile.write("\n")
                    outfile.write(f"Batch execution w/ strategy '{strategy_combination}' finished at {whole_end}, after {whole_elapsed_time}, with {len(succeeded)} success(es) and {len(failed)} failure(s).\n")
                    outfile.write(f"    - Succeeded case(s): {len(succeeded)}\n")
                    for each in succeeded:
                        outfile.write(f"        - {each}\n")
                    outfile.write(f"    - Failed case(s): {len(failed)}\n")
                    for each in failed:
                        outfile.write(f"        - {each}\n")

            print(f"| SPI  | Total Elapsed Time for strategy combination '{strategy_combination}': {whole_elapsed_time}")
            print(f"| SPI  | {len(succeeded)} succeeded, {len(failed)} failed.")

def check_directory_existence(directory_path):
    return os.path.exists(directory_path) and os.path.isdir(directory_path)
    


if __name__ == '__main__':
    main(sys.argv)
