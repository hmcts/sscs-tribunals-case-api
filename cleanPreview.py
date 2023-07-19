import os
import subprocess
import sys
import time


# TODO:
# Wort adding condition to check if given namespace exists. For example fetch the NS and check if given argument exists
# in the list


# Namespace as first argument
namespace = ""
if len(sys.argv) == 1:
    sys.exit("You need to provide the namespace and sudo password when running a script: \n"
             "'python3 cleanPreview.py <namespace> <pwd>'")

namespace = sys.argv[1]
pwd = sys.argv[2]

try:
    s = subprocess.check_output('docker ps', shell=True)
    print('Results of docker ps' + str(s))
except subprocess.CalledProcessError:
    print("Docker is not running")
    print("Starting docker daemon")

    os.system(f'echo {pwd} | sudo -S dockerd > /dev/null 2>&1 & >> ~/.zshrc')
    # Pause for 5s for the docker to start
    time.sleep(5)

# Just check again if docker is up!
subprocess.check_output('docker ps', shell=True)
try:
    logged_in = subprocess.check_output('az ad signed-in-user show', shell=True)
except subprocess.CalledProcessError:
    print('Login in to azure')
    os.system('az login')

    print('Login in HMCTSPUBLIC namescpace')
    os.system('az acr login -n hmctspublic')
    print('\nGetting list of pods...')

pods = os.popen(f'kubectl get pods -n {namespace} --no-headers -o custom-columns=":metadata.name"').read()
print('------------------LIST OF PODS---------------')
pods = pods.split('\n')
print('\n'.join(pods))

pr_numbers = []
for pod in pods:
    if pod.__contains__('-pr-'):
        last_index_of_pr = pod.rindex('-pr-') + 4
        pr_number = pod[last_index_of_pr:]
        pr_number = pr_number[:pr_number.index('-')]
        pr_numbers.append(pr_number)

to_delete = []
kubectl_delete_cmd = f'kubectl delete deployments -n {namespace} '
for pr_number in pr_numbers:
    if pr_numbers.count(pr_number) == 1:
        pod_name_to_delete_string = ""
        pod_name_to_delete = [s for s in pods if pr_number in s]
        if pod_name_to_delete[0].__contains__('java'):
            pod_name_to_delete_string = pod_name_to_delete[0][0:pod_name_to_delete[0].rindex('java') + 4]
        elif pod_name_to_delete[0].__contains__('nodejs'):
            pod_name_to_delete_string = pod_name_to_delete[0][0:pod_name_to_delete[0].rindex('nodejs') + 6]
        elif pod_name_to_delete[0].__contains__('redis'):
            pod_name_to_delete_string = pod_name_to_delete[0][0:pod_name_to_delete[0].rindex('redis') + 5]
        elif pod_name_to_delete[0].__contains__('postgresql'):
            pod_name_to_delete_string = pod_name_to_delete[0][0:pod_name_to_delete[0].rindex('postgresql') + 10]
        to_delete.append(pod_name_to_delete_string)
        if to_delete.count(pod_name_to_delete_string):
            if pod_name_to_delete_string.__contains__('redis') | pod_name_to_delete_string.__contains__('postgres'):
                kubectl_delete_cmd = kubectl_delete_cmd.replace("deployments", "sts")
        kubectl_delete_cmd = kubectl_delete_cmd + pod_name_to_delete_string + " "

if kubectl_delete_cmd.__contains__("-pr-"):
    print('---DELETING PODS---')
    print('\n'.join(to_delete))
    subprocess.check_output(kubectl_delete_cmd, shell=True)
else:
    print('Nothing to delete...')
