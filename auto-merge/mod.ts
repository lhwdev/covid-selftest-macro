// @deno-types=https://cdn.esm.sh/v53/@octokit/core@3.5.1/dist-types/index.d.ts
import { Octokit } from "https://cdn.skypack.dev/@octokit/core";
import config from "./config.ts";

function failSecurity(): never {
  Deno.exit()
}

function failIntendedSecurity(): never {
  Deno.exit(1)
}


function failCondition(): never {
  Deno.exit()
}

const [fullRepo, token, pullNumberStr] = Deno.args
const pullNumber = parseInt(pullNumberStr)
const [owner, repo] = fullRepo.split('/')
const octokit = new Octokit({ auth: token })

const pullInfo = await octokit.request("GET /repos/{owner}/{repo}/pulls/{pull_number}", {
  owner, repo,
  pull_number: pullNumber
})


const id = pullInfo.data.user?.id
if(id === undefined) failSecurity()

const allowedUser = config.allowedUsers.includes(id)
if(!allowedUser) failSecurity()

if(!pullInfo.data.body?.includes('automerge')) failCondition()

async function comment(str: string) {
  await octokit.request("POST /repos/{owner}/{repo}/issues/{pull_number}/comments", {
    owner, repo,
    pull_number: pullNumber,
    body: str
  })
}

const pullFiles = await octokit.request("GET /repos/{owner}/{repo}/pulls/{pull_number}/files", {
  owner, repo,
  pull_number: pullNumber
})

const files = pullFiles.data

if(files.length !== 1) {
  await comment('❌ 오직 허용된 파일을 편집했을 때만 automerge를 사용할 수 있어요.\n 허용된 파일: ' + config.allowedFiles.map(s => '`' + s + '`').join(', '))
  failIntendedSecurity()
}

const file = files[0]
if(!config.allowedFiles.includes(file.filename)) {
  await comment('❌ 오직 허용된 파일을 편집했을 때만 automerge를 사용할 수 있어요.\n 허용된 파일: ' + config.allowedFiles.map(s => '`' + s + '`').join(', '))
  failIntendedSecurity()
}


// TODO: more checks
// allowed operation here

await octokit.request("PUT /repos/{owner}/{repo}/pulls/{pull_number}/merge", {
  owner, repo,
  pull_number: pullNumber,
  commit_message: `Auto merge pull request #${pullNumberStr} from ${pullInfo.data.head.repo?.full_name}`
})
