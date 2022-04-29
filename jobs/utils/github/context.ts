// Originally pulled from https://github.com/JasonEtco/actions-toolkit/blob/main/src/context.ts
import { WebhookPayload } from "./interfaces.ts";

export class Context {
  /**
   * Webhook payload object that triggered the workflow
   */
  payload: WebhookPayload;

  eventName: string;
  sha: string;
  ref: string;
  workflow: string;
  action: string;
  actor: string;
  job: string;
  runNumber: number;
  runId: number;
  apiUrl: string;
  serverUrl: string;
  graphqlUrl: string;

  /**
   * Hydrate the context from the environment
   */
  constructor() {
    this.payload = {};
    if (Deno.env.get("GITHUB_EVENT_PATH")) {
      try {
        this.payload = JSON.parse(
          Deno.readTextFileSync(Deno.env.get("GITHUB_EVENT_PATH")!),
        );
      } catch (_) {
        const path = Deno.env.get("GITHUB_EVENT_PATH");
        console.log(`GITHUB_EVENT_PATH ${path} does not exist`);
      }
    }
    this.eventName = Deno.env.get("GITHUB_EVENT_NAME")!;
    this.sha = Deno.env.get("GITHUB_SHA")!;
    this.ref = Deno.env.get("GITHUB_REF")!;
    this.workflow = Deno.env.get("GITHUB_WORKFLOW")!;
    this.action = Deno.env.get("GITHUB_ACTION")!;
    this.actor = Deno.env.get("GITHUB_ACTOR")!;
    this.job = Deno.env.get("GITHUB_JOB")!;
    this.runNumber = parseInt(Deno.env.get("GITHUB_RUN_NUMBER")!, 10);
    this.runId = parseInt(Deno.env.get("GITHUB_RUN_ID")!, 10);
    this.apiUrl = Deno.env.get("GITHUB_API_URL") ?? `https://api.github.com`;
    this.serverUrl = Deno.env.get("GITHUB_SERVER_URL") ?? `https://github.com`;
    this.graphqlUrl = Deno.env.get("GITHUB_GRAPHQL_URL") ?? `https://api.github.com/graphql`;
  }

  get issue(): { owner: string; repo: string; number: number } {
    const payload = this.payload;

    return {
      ...this.repo,
      number: (payload.issue || payload.pull_request || payload).number,
    };
  }

  get repo(): { owner: string; repo: string } {
    if (Deno.env.get("GITHUB_REPOSITORY")) {
      const [owner, repo] = Deno.env.get("GITHUB_REPOSITORY")!.split("/");
      return { owner, repo };
    }

    if (this.payload.repository) {
      return {
        owner: this.payload.repository.owner.login,
        repo: this.payload.repository.name,
      };
    }

    throw new Error(
      "context.repo requires a GITHUB_REPOSITORY environment variable like 'owner/repo'",
    );
  }
}
