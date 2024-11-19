import csv
import os
import subprocess
import re
import time

def run_defects4j_command(cmd):
    """Run a Defects4J command and return the output."""
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command: {cmd}\n{e}")
        return None

def checkout_project(project_id, bug_id, version_type, work_dir, subproject_locator):
    """Checkout the project with the specified bug id and version."""
    if subproject_locator != None:
        cmd = ["defects4j", "checkout", "-p", project_id, "-v", f"{bug_id}{version_type}", "-w", work_dir, "-s", subproject_locator]
    else:
        cmd = ["defects4j", "checkout", "-p", project_id, "-v", f"{bug_id}{version_type}", "-w", work_dir]
    return run_defects4j_command(cmd)

def get_bug_info(project_id, bug_id):
    """Get information about a specific bug."""
    cmd = ["defects4j", "info", "-p", project_id, "-b", str(bug_id)]
    return run_defects4j_command(cmd)

def diff_files(file_buggy, file_fixed):
    """Run diff between buggy and fixed files, ensuring only single-line changes, ignoring import edits, and capturing line numbers."""
    cmd = ["diff", file_buggy, file_fixed]
    diff_output = run_defects4j_command(cmd)
    
    added_lines = 0
    deleted_lines = 0
    change_line_numbers = []

    current_line_number = None

    for line in diff_output.splitlines():
        if re.match(r'^\d+[acd]\d+', line):
            current_line_number = int(re.split(r'[acd]', line)[0])
            continue

        if 'import' in line:
            continue

        if line.startswith(">"):
            added_lines += 1
            change_line_numbers.append(current_line_number)
        elif line.startswith("<"):
            deleted_lines += 1
            change_line_numbers.append(current_line_number)

    if added_lines > 1 or deleted_lines > 1:
        print("Skipping patch due to multiple line changes.")
        return None

    if added_lines >= 1 and deleted_lines == 0:
        print("Skipping patch due to omission fault.")
        return None

    if not change_line_numbers:
        print("No changed lines in diff")
        return None
    return change_line_numbers[0]

def try_file_paths(work_dir, faulty_file):
    """Recursively traverse all directories under work_dir to find the faulty file."""
    for root, dirs, files in os.walk(work_dir):
        if faulty_file in files:
            return os.path.join(root, faulty_file)
    return None

def process_project(project_id, subproject_locator, bug_id, csv_writer):
    """Process each project to find the faulty file path and line number."""
    work_dir_buggy = f"/home/young170/SPI_3.0/components/commit_collector/GrowBugs/tmp/buggy"
    work_dir_fixed = f"/home/young170/SPI_3.0/components/commit_collector/GrowBugs/tmp/fixed"

    checkout_project(project_id, bug_id, 'b', work_dir_buggy, subproject_locator)
    checkout_project(project_id, bug_id, 'f', work_dir_fixed, subproject_locator)

    bug_info = get_bug_info(project_id, bug_id)
    modified_sources = []
    modified_sources_start = False
    faulty_file = None

    for line in bug_info.splitlines():
        if "modified sources" in line:
            modified_sources_start = True
            continue

        if modified_sources_start:
            if not line.startswith(" - "):
                break
            faulty_class = line.split("-")[1].strip()
            modified_sources.append(faulty_class)

    if len(modified_sources) > 1:
        print(f"Skipping {project_id} Bug ID {bug_id} due to multiple modified sources.")
        return

    buffer = faulty_class.split('.')
    faulty_file = buffer[-1] + ".java"

    print("faulty_file: ", faulty_file)

    if faulty_file:
        buggy_file_path = try_file_paths(work_dir_buggy, faulty_file)
        fixed_file_path = try_file_paths(work_dir_fixed, faulty_file)

        if buggy_file_path is None:
            print("Failed to find the buggy file")
            return

        faulty_line = diff_files(buggy_file_path, fixed_file_path)
        print("faulty_line: ", faulty_line)

        if faulty_line:
            csv_writer.writerow([bug_id, buggy_file_path, faulty_line, faulty_line])
            print(f"Saved Bug ID {bug_id} to CSV.")
        else:
            print(f"Failed to process {project_id} Bug ID {bug_id}.")

def parse_bugs(bug_string):
    """Parse the bug ID string (e.g., "1-5,7-9,<br/>22,25") into a list of individual bug IDs."""
    bug_string = re.sub(r'</?br\s*/?>', '', bug_string)
    bugs = []
    parts = bug_string.split(',')
    for part in parts:
        if '-' in part:
            start, end = map(int, part.split('-'))
            bugs.extend(range(start, end + 1))
        else:
            bugs.append(int(part))
    return bugs

def read_table_from_readme(file_path):
    """Read the markdown table from a README.md file and extract Project ID, Subproject Locator, and Bug IDs."""
    projects = {}
    with open(file_path, 'r') as file:
        in_table = False
        for line in file:
            if '|' in line:
                if 'Project ID' in line:
                    in_table = False
                    continue
                if '---------' in line:
                    in_table = True
                    continue

                if in_table:
                    columns = [col.strip() for col in line.split('|') if col.strip()]
                    if len(columns) >= 5:
                        project_id = re.sub(r'</?br\s*/?>', '', columns[1])
                        subproject_locator = None
                        if len(columns) == 6:
                            subproject_locator = columns[3].split('<br/>')[0]
                            bug_ids = parse_bugs(columns[5])
                        else:
                            bug_ids = parse_bugs(columns[4])
                        projects[project_id] = (subproject_locator, bug_ids)
    return projects

def main():
    file_path = '/home/young170/SPI_3.0/components/commit_collector/GrowBugs/README.md'
    projects = read_table_from_readme(file_path)

    defects4j_projects = ["Chart", "Cli", "Closure", "Codec", "Collections", "Compress", "Csv", "Gson", "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Lang", "Math", "Mockito", "Time"]

    for project_id, (subproject_locator, bug_ids) in projects.items():
        # if project_id not in defects4j_projects:
        #     continue

        time.sleep(3)

        csv_file = f"{project_id}.csv"
        file_exists = os.path.isfile(csv_file)

        with open(csv_file, mode='a', newline='') as file:
            writer = csv.writer(file)

            if not file_exists:
                writer.writerow(["Defects4J ID", "Faulty file path", "fix faulty line", "blame faulty line"])  # CSV header

            for bug_id in bug_ids:
                print(f"Processing {project_id} Bug ID {bug_id}...")
                rm_cmd = ["rm", "-rf", "/home/young170/SPI_3.0/components/commit_collector/GrowBugs/tmp/*"]
                try:
                    subprocess.run(rm_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
                except subprocess.CalledProcessError as e:
                    print(f"Error running command: {rm_cmd}\n{e}")
                    continue
                time.sleep(3)

                process_project(project_id, subproject_locator, bug_id, writer)

if __name__ == "__main__":
    main()
