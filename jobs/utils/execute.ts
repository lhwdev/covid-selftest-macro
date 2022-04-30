import { ensureDirSync } from "https://deno.land/std@0.128.0/fs/mod.ts";
import { resolve } from "https://deno.land/std@0.128.0/path/mod.ts";
import { readLines } from "https://deno.land/std@0.128.0/io/mod.ts";

type Options = {
  cmd: string[];
  context?: ExecContext;
};

type ExecContextParam = {
  cwd?: string;
};

export function executeAsync(options: Options | string[]): Process {
  if (Array.isArray(options)) {
    return executeAsync({ cmd: options });
  }

  return new Process(options);
}

export function execContext(context: ExecContextParam): ExecContext {
  return new ExecContext(context);
}

type StdIo = number | "inherit" | "piped";

export class Process {
  private stdOptions: {
    out: StdIo;
  };
  private process: Deno.Process | undefined = undefined;

  constructor(private options: Options) {
    this.stdOptions = { out: "inherit" };
  }

  private createProcess() {
    const process = Deno.run({
      cmd: [...this.options.cmd],
      cwd: this.options.context?.cwd,
      stdout: this.stdOptions.out,
    });
    this.process = process;
    return process;
  }

  private getProcess() {
    return this.process ?? this.createProcess();
  }

  async join(): Promise<number> {
    const process = this.getProcess();

    const status = await process.status();
    if (!status.success) {
      throw new Error(
        `Process ${this.options.cmd} failed with code ${status.code}`,
      );
    }
    return status.code;
  }

  async resultText(): Promise<string> {
    this.stdOptions.out = "piped";
    const process = this.getProcess();

    let text = "";
    for await (const line of readLines(process.stdout!)) {
      text += line;
      text += "\n";
    }
    await this.join();
    return text;
  }
}

export class ExecContext {
  constructor(private param: ExecContextParam) {}

  cd(path: string): ExecContext {
    const newCwd = resolve(this.param.cwd ?? ".", path);
    ensureDirSync(newCwd);
    return new ExecContext({
      ...this.param,
      cwd: newCwd,
    });
  }

  executeAsync(options: Options | string[]): Process {
    if (Array.isArray(options)) {
      return executeAsync({ cmd: options, context: this });
    } else {
      return executeAsync({ ...options, context: this });
    }
  }

  execute(options: Options | string[]): Promise<number> {
    return this.executeAsync(options).join();
  }

  get cwd(): string {
    return this.param.cwd ?? Deno.cwd();
  }
}

export const exec = execContext({});
