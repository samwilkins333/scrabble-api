import * as request from "request-promise";

const { origin } = window.location;

export function url(target: string) {
    return `url(images/${target})`;
}

export function src(target: string) {
    return `/images/${target}`;
}

export namespace Server {

    export async function Post(relativeRoute: string, body: any) {
        return handleRequest({
            method: "POST",
            uri: origin + relativeRoute,
            json: true,
            body
        });
    }

    export async function Get(relativeRoute: string) {
        return handleRequest({ uri: origin + relativeRoute });
    }

    async function handleRequest(parameters: request.OptionsWithUri) {
        const response = await request(parameters);
        if (typeof response === "string") {
            try {
                return JSON.parse(response);
            } catch {
                return response;
            }
        }
        return response;
    }

} 