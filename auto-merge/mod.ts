import { Octokit } from "https://esm.sh/@octokit/core";

function failSecurity(): never {
  Deno.exit()
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

// See http://caius.github.io/github_id
const allowedUsers = [
  36781325, // lhwdev
  74242561, // cog25
  87003194 // blluv
]
const id = pullInfo.data.user?.id
if(id === undefined) failSecurity()

const allowedUser = allowedUsers.includes(id)
if(!allowedUser) failSecurity()

if(!pullInfo.data.body?.includes('automerge')) failCondition()

const pullFiles = await octokit.request("GET /repos/{owner}/{repo}/pulls/{pull_number}/files", {
  owner, repo,
  pull_number: pullNumber
})

const files = pullFiles.data
if(files.length !== 1) failSecurity()

const file = files[0]
const allowedFiles = ["src/info/special-thanks.json"]
if(!allowedFiles.includes(file.filename)) failSecurity()


// TODO: more checks
// allowed operation here

await octokit.request("PUT /repos/{owner}/{repo}/pulls/{pull_number}/merge", {
  owner, repo,
  pull_number: pullNumber,
  commit_message: `Auto merge pull request #${pullNumberStr} from ${pullInfo.data.head.repo?.full_name}`
})
