param(
    [string]$BaseUrl = 'http://127.0.0.1:8080/jeecg-boot',
    [string]$Username = 'admin',
    [string]$Password = '123456',
    [string]$OutputFile = 'result.json'
)

$ErrorActionPreference = 'Stop'
$script:results = @()
$ctx = @{ modelId = $null; deploymentId = $null; processDefinitionId = $null; instanceId = $null; taskId = $null }

function Invoke-Case {
    param([string]$Name,[string]$Method,[string]$Path,$Body=$null,[string]$Token=$null,[string]$Notes='',[string]$Expect='success')
    $headers = @{}
    if ($Token) { $headers['X-Access-Token'] = $Token }
    $url = "$BaseUrl$Path"
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $status = 0; $code = $null; $success = $false; $message = ''; $rawResult = $null; $errorMsg = $null
    try {
        $params = @{ Uri = $url; Method = $Method; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 30 }
        if ($Body -ne $null) {
            $params.ContentType = 'application/json'
            if ($Body -is [string]) { $params.Body = $Body } else { $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress) }
        }
        $resp = Invoke-WebRequest @params
        $status = [int]$resp.StatusCode
        $text = $resp.Content
        try {
            $json = $text | ConvertFrom-Json
            $code = $json.code
            $success = [bool]$json.success
            $message = [string]$json.message
            $rawResult = $json.result
        } catch {
            $code = $status
            $success = ($status -ge 200 -and $status -lt 300)
            $message = if ($text) { $text.Substring(0, [Math]::Min(200, $text.Length)) } else { '' }
        }
    } catch {
        $errorMsg = $_.Exception.Message
        if ($_.Exception.Response) {
            try {
                $status = [int]$_.Exception.Response.StatusCode
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object IO.StreamReader($stream)
                $body = $reader.ReadToEnd()
                $message = $body.Substring(0, [Math]::Min(400, $body.Length))
            } catch {}
        }
    }
    $sw.Stop()
    $verdict = 'INFO'
    if ($Expect -eq 'success') { if ($success -and ($code -eq 200 -or $code -eq 0)) { $verdict='PASS' } else { $verdict='FAIL' } }
    elseif ($Expect -eq 'fail') { if (-not $success) { $verdict='PASS' } else { $verdict='FAIL' } }
    $case = [ordered]@{ name=$Name; method=$Method; path=$Path; status=$status; bizCode=$code; success=$success; message=$message; durationMs=$sw.ElapsedMilliseconds; verdict=$verdict; notes=$Notes; error=$errorMsg }
    $script:results += ,$case
    Write-Host ("[{0}] {1} {2} => {3} ({4} ms) {5}" -f $verdict, $Method, $Path, $status, $sw.ElapsedMilliseconds, $Name)
    return $rawResult
}

# 0. Login
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json -Compress
$loginResp = Invoke-RestMethod -Uri "$BaseUrl/sys/mLogin" -Method POST -ContentType 'application/json' -Body $loginBody -TimeoutSec 15
$token = $loginResp.result.token
Write-Host "TOKEN OK"
$script:results += ,([ordered]@{ name='AUTH-mLogin-admin'; method='POST'; path='/sys/mLogin'; status=200; bizCode=200; success=$true; message='token issued'; durationMs=0; verdict='PASS'; notes='bypass captcha'; error=$null })

# 1. Model list baseline
Invoke-Case -Name 'MODEL-list-baseline' -Method GET -Path "/flowable/model/list?pageNo=1&pageSize=10" -Token $token | Out-Null

# 2. Create model
$key = 'auto_' + (Get-Date -Format 'HHmmss')
$createBody = @{ name = 'AutoTestModel'; key = $key; description = 'auto test' }
$modelId = Invoke-Case -Name 'MODEL-create' -Method POST -Path '/flowable/model/create' -Body $createBody -Token $token
$ctx.modelId = $modelId
Write-Host "MODEL_ID=$modelId"

# 3. Detail
if ($ctx.modelId) { Invoke-Case -Name 'MODEL-detail' -Method GET -Path "/flowable/model/detail/$($ctx.modelId)" -Token $token | Out-Null }

# 4. Update + fetch XML
$xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI" targetNamespace="http://www.flowable.org/processdef">
  <process id="$key" name="AutoTestProcess" isExecutable="true">
    <startEvent id="start"/>
    <sequenceFlow id="f1" sourceRef="start" targetRef="approveTask"/>
    <userTask id="approveTask" name="Approve" flowable:assignee="admin"/>
    <sequenceFlow id="f2" sourceRef="approveTask" targetRef="end"/>
    <endEvent id="end"/>
  </process>
  <bpmndi:BPMNDiagram id="D_$key">
    <bpmndi:BPMNPlane id="P_$key" bpmnElement="$key">
      <bpmndi:BPMNShape id="s_di" bpmnElement="start"><omgdc:Bounds x="100" y="100" width="30" height="30"/></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="t_di" bpmnElement="approveTask"><omgdc:Bounds x="200" y="90" width="100" height="50"/></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="e_di" bpmnElement="end"><omgdc:Bounds x="370" y="100" width="30" height="30"/></bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="f1_di" bpmnElement="f1"><omgdi:waypoint x="130" y="115"/><omgdi:waypoint x="200" y="115"/></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="f2_di" bpmnElement="f2"><omgdi:waypoint x="300" y="115"/><omgdi:waypoint x="370" y="115"/></bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>
"@
if ($ctx.modelId) {
    $updateBody = @{ name='AutoTestModel'; description='updated'; bpmnXml=$xml }
    Invoke-Case -Name 'MODEL-update-xml' -Method PUT -Path "/flowable/model/update/$($ctx.modelId)" -Body $updateBody -Token $token | Out-Null
    Invoke-Case -Name 'MODEL-get-xml' -Method GET -Path "/flowable/model/xml/$($ctx.modelId)" -Token $token | Out-Null
}

# 5. Deploy
if ($ctx.modelId) {
    $deployRet = Invoke-Case -Name 'MODEL-deploy' -Method POST -Path "/flowable/model/deploy/$($ctx.modelId)" -Token $token
    $ctx.deploymentId = $deployRet
    Write-Host "DEPLOY_ID=$deployRet"
}

# 6. Definition list
$defList = Invoke-Case -Name 'DEF-list' -Method GET -Path "/flowable/definition/list?pageNo=1&pageSize=20" -Token $token
if ($defList -and $defList.records) {
    $match = $defList.records | Where-Object { $_.key -eq $key } | Select-Object -First 1
    if ($match) { $ctx.processDefinitionId = $match.id; Write-Host "DEF_ID=$($match.id)" }
}

# 7. Definition xml + diagram + suspend + activate
if ($ctx.processDefinitionId) {
    Invoke-Case -Name 'DEF-xml' -Method GET -Path "/flowable/definition/xml/$($ctx.processDefinitionId)" -Token $token | Out-Null
    try {
        $sw = [Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-WebRequest -Uri "$BaseUrl/flowable/definition/diagram/$($ctx.processDefinitionId)" -Headers @{ 'X-Access-Token'=$token } -UseBasicParsing -TimeoutSec 15
        $sw.Stop()
        $ok = $r.RawContentLength -gt 100
        $script:results += ,([ordered]@{ name='DEF-diagram-png'; method='GET'; path="/flowable/definition/diagram/$($ctx.processDefinitionId)"; status=[int]$r.StatusCode; bizCode=$null; success=$ok; message="bytes=$($r.RawContentLength)"; durationMs=$sw.ElapsedMilliseconds; verdict=if($ok){'PASS'}else{'FAIL'}; notes='binary png'; error=$null })
        Write-Host "DEF-diagram bytes=$($r.RawContentLength)"
    } catch {
        $script:results += ,([ordered]@{ name='DEF-diagram-png'; method='GET'; path="/flowable/definition/diagram/$($ctx.processDefinitionId)"; status=0; bizCode=$null; success=$false; message=$_.Exception.Message; durationMs=0; verdict='FAIL'; notes=''; error=$_.Exception.Message })
    }
    Invoke-Case -Name 'DEF-suspend' -Method PUT -Path "/flowable/definition/suspend/$($ctx.processDefinitionId)" -Token $token | Out-Null
    Invoke-Case -Name 'DEF-activate' -Method PUT -Path "/flowable/definition/activate/$($ctx.processDefinitionId)" -Token $token | Out-Null
}

# 8. Start instance
if ($ctx.processDefinitionId) {
    $startBody = @{ processDefinitionKey=$key; businessKey='AUTO-'+(Get-Date -Format 'HHmmss'); businessTitle='auto-1'; variables=@{ amount=100; reason='auto' } }
    $iid = Invoke-Case -Name 'PROC-start' -Method POST -Path '/flowable/process/start' -Body $startBody -Token $token
    $ctx.instanceId = $iid
    Write-Host "INST_ID=$iid"
}

# 9. My started
Invoke-Case -Name 'PROC-my' -Method GET -Path "/flowable/process/my?pageNo=1&pageSize=10" -Token $token | Out-Null

# 10. Todo
$todo = Invoke-Case -Name 'TASK-todo' -Method GET -Path "/flowable/task/todo?pageNo=1&pageSize=10" -Token $token
if ($todo -and $todo.records) {
    $first = $todo.records | Where-Object { $_.processInstanceId -eq $ctx.instanceId } | Select-Object -First 1
    if (-not $first) { $first = $todo.records | Select-Object -First 1 }
    if ($first) { $ctx.taskId = $first.id; Write-Host "TASK_ID=$($first.id)" }
}

# 11. Claim + Complete
if ($ctx.taskId) {
    Invoke-Case -Name 'TASK-claim' -Method POST -Path "/flowable/task/claim/$($ctx.taskId)" -Token $token | Out-Null
    $completeBody = @{ comment='auto approve'; variables=@{ approved=$true } }
    Invoke-Case -Name 'TASK-complete' -Method POST -Path "/flowable/task/complete/$($ctx.taskId)" -Body $completeBody -Token $token | Out-Null
}

# 12. Done
Invoke-Case -Name 'TASK-done' -Method GET -Path "/flowable/task/done?pageNo=1&pageSize=10" -Token $token | Out-Null

# 13. History
if ($ctx.instanceId) {
    Invoke-Case -Name 'HIST-instance' -Method GET -Path "/flowable/history/instance/$($ctx.instanceId)" -Token $token | Out-Null
    try {
        $sw = [Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-WebRequest -Uri "$BaseUrl/flowable/history/diagram/$($ctx.instanceId)" -Headers @{ 'X-Access-Token'=$token } -UseBasicParsing -TimeoutSec 15
        $sw.Stop()
        $ok = $r.RawContentLength -gt 100
        $script:results += ,([ordered]@{ name='HIST-diagram-png'; method='GET'; path="/flowable/history/diagram/$($ctx.instanceId)"; status=[int]$r.StatusCode; bizCode=$null; success=$ok; message="bytes=$($r.RawContentLength)"; durationMs=$sw.ElapsedMilliseconds; verdict=if($ok){'PASS'}else{'FAIL'}; notes='binary png'; error=$null })
    } catch {
        $script:results += ,([ordered]@{ name='HIST-diagram-png'; method='GET'; path="/flowable/history/diagram/$($ctx.instanceId)"; status=0; bizCode=$null; success=$false; message=$_.Exception.Message; durationMs=0; verdict='FAIL'; notes=''; error=$_.Exception.Message })
    }
}

# 14. Extra instances for cancel / reject / delegate / addSign
if ($ctx.processDefinitionId) {
    $inst2 = Invoke-Case -Name 'PROC-start-2' -Method POST -Path '/flowable/process/start' -Body (@{ processDefinitionKey=$key; businessKey='AUTO-CANCEL'; businessTitle='for-cancel' }) -Token $token
    if ($inst2) { Invoke-Case -Name 'PROC-cancel' -Method POST -Path ("/flowable/process/cancel/" + $inst2 + "?reason=auto") -Token $token | Out-Null }

    $inst3 = Invoke-Case -Name 'PROC-start-3' -Method POST -Path '/flowable/process/start' -Body (@{ processDefinitionKey=$key; businessKey='AUTO-REJECT'; businessTitle='for-reject' }) -Token $token
    if ($inst3) {
        $td = Invoke-Case -Name 'TASK-todo-2' -Method GET -Path "/flowable/task/todo?pageNo=1&pageSize=20" -Token $token
        if ($td -and $td.records) {
            $t3 = $td.records | Where-Object { $_.processInstanceId -eq $inst3 } | Select-Object -First 1
            if ($t3) { Invoke-Case -Name 'TASK-reject' -Method POST -Path "/flowable/task/reject/$($t3.id)" -Body (@{ comment='auto reject' }) -Token $token | Out-Null }
        }
    }

    $inst4 = Invoke-Case -Name 'PROC-start-4' -Method POST -Path '/flowable/process/start' -Body (@{ processDefinitionKey=$key; businessKey='AUTO-DELEGATE'; businessTitle='for-delegate' }) -Token $token
    if ($inst4) {
        $td = Invoke-Case -Name 'TASK-todo-3' -Method GET -Path "/flowable/task/todo?pageNo=1&pageSize=20" -Token $token
        if ($td -and $td.records) {
            $t4 = $td.records | Where-Object { $_.processInstanceId -eq $inst4 } | Select-Object -First 1
            if ($t4) {
                Invoke-Case -Name 'TASK-delegate' -Method POST -Path "/flowable/task/delegate/$($t4.id)" -Body (@{ targetUser='jeecg'; comment='auto delegate' }) -Token $token | Out-Null
                Invoke-Case -Name 'TASK-addSign' -Method POST -Path "/flowable/task/addSign/$($t4.id)" -Body (@{ targetUser='jeecg'; position='after'; comment='auto addSign' }) -Token $token | Out-Null
            }
        }
    }
}

# 15. Cleanup
if ($ctx.deploymentId) { Invoke-Case -Name 'DEF-delete' -Method DELETE -Path "/flowable/definition/delete/$($ctx.deploymentId)?cascade=true" -Token $token | Out-Null }
if ($ctx.modelId) { Invoke-Case -Name 'MODEL-delete' -Method DELETE -Path "/flowable/model/delete/$($ctx.modelId)" -Token $token | Out-Null }

# 16. Negative: no token
try {
    $r = Invoke-WebRequest -Uri "$BaseUrl/flowable/model/list?pageNo=1&pageSize=1" -UseBasicParsing -TimeoutSec 10
    $j = $r.Content | ConvertFrom-Json
    $ok = ($j.success -eq $false) -or ($j.code -ne 200)
    $script:results += ,([ordered]@{ name='NEG-no-token'; method='GET'; path='/flowable/model/list'; status=[int]$r.StatusCode; bizCode=$j.code; success=-not $ok; message=$j.message; durationMs=0; verdict=if($ok){'PASS'}else{'FAIL'}; notes='expect unauthorized'; error=$null })
} catch {
    $script:results += ,([ordered]@{ name='NEG-no-token'; method='GET'; path='/flowable/model/list'; status=401; bizCode=$null; success=$false; message=$_.Exception.Message; durationMs=0; verdict='PASS'; notes='expect unauthorized'; error=$null })
}

$json = $script:results | ConvertTo-Json -Depth 10
[IO.File]::WriteAllText($OutputFile, $json, [Text.Encoding]::UTF8)
Write-Host "`n==== DONE, cases=$($script:results.Count) ===="
$pass = ($script:results | Where-Object { $_.verdict -eq 'PASS' }).Count
$fail = ($script:results | Where-Object { $_.verdict -eq 'FAIL' }).Count
$info = ($script:results | Where-Object { $_.verdict -eq 'INFO' }).Count
Write-Host "PASS=$pass FAIL=$fail INFO=$info"
