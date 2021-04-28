/* tslint:disable */
/* eslint-disable */
/**
 * Xtages Internal API
 * Xtages internal API. Some of these endpoints might be extracted to an external API in the future. 
 *
 * The version of the OpenAPI document: 0.0.1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import { Configuration } from './configuration';
import globalAxios, { AxiosPromise, AxiosInstance } from 'axios';
// Some imports not used depending on template conditions
// @ts-ignore
import { DUMMY_BASE_URL, assertParamExists, setApiKeyToObject, setBasicAuthToObject, setBearerAuthToObject, setOAuthToObject, setSearchParams, serializeDataIfNeeded, toPathString, createRequestFunction } from './common';
// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS, RequestArgs, BaseAPI, RequiredError } from './base';

/**
 * Request made to POST /checkout/session
 * @export
 * @interface CreateCheckoutSessionReq
 */
export interface CreateCheckoutSessionReq {
    /**
     * 
     * @type {string}
     * @memberof CreateCheckoutSessionReq
     */
    organizationName: string;
    /**
     * 
     * @type {Array<string>}
     * @memberof CreateCheckoutSessionReq
     */
    priceIds: Array<string>;
}
/**
 * Request made to POST /organization
 * @export
 * @interface CreateOrgReq
 */
export interface CreateOrgReq {
    /**
     * 
     * @type {string}
     * @memberof CreateOrgReq
     */
    organizationName: string;
    /**
     * 
     * @type {string}
     * @memberof CreateOrgReq
     */
    ownerCognitoUserId: string;
}
/**
 * Request made to POST /project
 * @export
 * @interface CreateProjectReq
 */
export interface CreateProjectReq {
    /**
     * 
     * @type {string}
     * @memberof CreateProjectReq
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProjectReq
     */
    type: CreateProjectReqTypeEnum;
    /**
     * 
     * @type {string}
     * @memberof CreateProjectReq
     */
    version: string;
    /**
     * 
     * @type {boolean}
     * @memberof CreateProjectReq
     */
    passCheckRuleEnabled?: boolean;
}

/**
    * @export
    * @enum {string}
    */
export enum CreateProjectReqTypeEnum {
    Nodejs = 'NODEJS'
}

/**
 * General API error
 * @export
 * @interface ErrorDesc
 */
export interface ErrorDesc {
    /**
     * 
     * @type {number}
     * @memberof ErrorDesc
     */
    code: number;
    /**
     * 
     * @type {string}
     * @memberof ErrorDesc
     */
    message: string;
}
/**
 * An Organization
 * @export
 * @interface Organization
 */
export interface Organization {
    /**
     * 
     * @type {string}
     * @memberof Organization
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof Organization
     */
    subscription_status?: OrganizationSubscriptionStatusEnum;
}

/**
    * @export
    * @enum {string}
    */
export enum OrganizationSubscriptionStatusEnum {
    Unconfirmed = 'UNCONFIRMED',
    Active = 'ACTIVE',
    Suspended = 'SUSPENDED',
    PendingCancellation = 'PENDING_CANCELLATION',
    Cancelled = 'CANCELLED'
}

/**
 * An Xtages project
 * @export
 * @interface Project
 */
export interface Project {
    /**
     * 
     * @type {number}
     * @memberof Project
     */
    id?: number;
    /**
     * 
     * @type {string}
     * @memberof Project
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof Project
     */
    type?: ProjectTypeEnum;
    /**
     * 
     * @type {string}
     * @memberof Project
     */
    version?: string;
    /**
     * 
     * @type {boolean}
     * @memberof Project
     */
    pass_check_rule_enabled?: boolean;
}

/**
    * @export
    * @enum {string}
    */
export enum ProjectTypeEnum {
    Nodejs = 'NODEJS'
}


/**
 * CheckoutApi - axios parameter creator
 * @export
 */
export const CheckoutApiAxiosParamCreator = function (configuration?: Configuration) {
    return {
        /**
         * 
         * @summary Creates a Stripe Checkout session
         * @param {CreateCheckoutSessionReq} createCheckoutSessionReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createCheckoutSession: async (createCheckoutSessionReq: CreateCheckoutSessionReq, options: any = {}): Promise<RequestArgs> => {
            // verify required parameter 'createCheckoutSessionReq' is not null or undefined
            assertParamExists('createCheckoutSession', 'createCheckoutSessionReq', createCheckoutSessionReq)
            const localVarPath = `/checkout/session`;
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'POST', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            localVarHeaderParameter['Content-Type'] = 'application/json';

            setSearchParams(localVarUrlObj, localVarQueryParameter, options.query);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};
            localVarRequestOptions.data = serializeDataIfNeeded(createCheckoutSessionReq, localVarRequestOptions, configuration)

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
        /**
         * 
         * @summary Gets a Stripe customer portal session URI
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        getCustomerPortalSession: async (options: any = {}): Promise<RequestArgs> => {
            const localVarPath = `/checkout/portal/session`;
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'POST', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            setSearchParams(localVarUrlObj, localVarQueryParameter, options.query);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
    }
};

/**
 * CheckoutApi - functional programming interface
 * @export
 */
export const CheckoutApiFp = function(configuration?: Configuration) {
    const localVarAxiosParamCreator = CheckoutApiAxiosParamCreator(configuration)
    return {
        /**
         * 
         * @summary Creates a Stripe Checkout session
         * @param {CreateCheckoutSessionReq} createCheckoutSessionReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async createCheckoutSession(createCheckoutSessionReq: CreateCheckoutSessionReq, options?: any): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<string>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.createCheckoutSession(createCheckoutSessionReq, options);
            return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration);
        },
        /**
         * 
         * @summary Gets a Stripe customer portal session URI
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async getCustomerPortalSession(options?: any): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<string>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.getCustomerPortalSession(options);
            return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration);
        },
    }
};

/**
 * CheckoutApi - factory interface
 * @export
 */
export const CheckoutApiFactory = function (configuration?: Configuration, basePath?: string, axios?: AxiosInstance) {
    const localVarFp = CheckoutApiFp(configuration)
    return {
        /**
         * 
         * @summary Creates a Stripe Checkout session
         * @param {CreateCheckoutSessionReq} createCheckoutSessionReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createCheckoutSession(createCheckoutSessionReq: CreateCheckoutSessionReq, options?: any): AxiosPromise<string> {
            return localVarFp.createCheckoutSession(createCheckoutSessionReq, options).then((request) => request(axios, basePath));
        },
        /**
         * 
         * @summary Gets a Stripe customer portal session URI
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        getCustomerPortalSession(options?: any): AxiosPromise<string> {
            return localVarFp.getCustomerPortalSession(options).then((request) => request(axios, basePath));
        },
    };
};

/**
 * CheckoutApi - object-oriented interface
 * @export
 * @class CheckoutApi
 * @extends {BaseAPI}
 */
export class CheckoutApi extends BaseAPI {
    /**
     * 
     * @summary Creates a Stripe Checkout session
     * @param {CreateCheckoutSessionReq} createCheckoutSessionReq 
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof CheckoutApi
     */
    public createCheckoutSession(createCheckoutSessionReq: CreateCheckoutSessionReq, options?: any) {
        return CheckoutApiFp(this.configuration).createCheckoutSession(createCheckoutSessionReq, options).then((request) => request(this.axios, this.basePath));
    }

    /**
     * 
     * @summary Gets a Stripe customer portal session URI
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof CheckoutApi
     */
    public getCustomerPortalSession(options?: any) {
        return CheckoutApiFp(this.configuration).getCustomerPortalSession(options).then((request) => request(this.axios, this.basePath));
    }
}


/**
 * OrganizationApi - axios parameter creator
 * @export
 */
export const OrganizationApiAxiosParamCreator = function (configuration?: Configuration) {
    return {
        /**
         * 
         * @summary Creates a new Organization beloging to a user, if the user doesn\'t exist then it\'s created also
         * @param {CreateOrgReq} createOrgReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createOrganization: async (createOrgReq: CreateOrgReq, options: any = {}): Promise<RequestArgs> => {
            // verify required parameter 'createOrgReq' is not null or undefined
            assertParamExists('createOrganization', 'createOrgReq', createOrgReq)
            const localVarPath = `/organization`;
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'POST', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            localVarHeaderParameter['Content-Type'] = 'application/json';

            setSearchParams(localVarUrlObj, localVarQueryParameter, options.query);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};
            localVarRequestOptions.data = serializeDataIfNeeded(createOrgReq, localVarRequestOptions, configuration)

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
    }
};

/**
 * OrganizationApi - functional programming interface
 * @export
 */
export const OrganizationApiFp = function(configuration?: Configuration) {
    const localVarAxiosParamCreator = OrganizationApiAxiosParamCreator(configuration)
    return {
        /**
         * 
         * @summary Creates a new Organization beloging to a user, if the user doesn\'t exist then it\'s created also
         * @param {CreateOrgReq} createOrgReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async createOrganization(createOrgReq: CreateOrgReq, options?: any): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Organization>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.createOrganization(createOrgReq, options);
            return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration);
        },
    }
};

/**
 * OrganizationApi - factory interface
 * @export
 */
export const OrganizationApiFactory = function (configuration?: Configuration, basePath?: string, axios?: AxiosInstance) {
    const localVarFp = OrganizationApiFp(configuration)
    return {
        /**
         * 
         * @summary Creates a new Organization beloging to a user, if the user doesn\'t exist then it\'s created also
         * @param {CreateOrgReq} createOrgReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createOrganization(createOrgReq: CreateOrgReq, options?: any): AxiosPromise<Organization> {
            return localVarFp.createOrganization(createOrgReq, options).then((request) => request(axios, basePath));
        },
    };
};

/**
 * OrganizationApi - object-oriented interface
 * @export
 * @class OrganizationApi
 * @extends {BaseAPI}
 */
export class OrganizationApi extends BaseAPI {
    /**
     * 
     * @summary Creates a new Organization beloging to a user, if the user doesn\'t exist then it\'s created also
     * @param {CreateOrgReq} createOrgReq 
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof OrganizationApi
     */
    public createOrganization(createOrgReq: CreateOrgReq, options?: any) {
        return OrganizationApiFp(this.configuration).createOrganization(createOrgReq, options).then((request) => request(this.axios, this.basePath));
    }
}


/**
 * ProjectApi - axios parameter creator
 * @export
 */
export const ProjectApiAxiosParamCreator = function (configuration?: Configuration) {
    return {
        /**
         * 
         * @summary Creates a new Xtages project
         * @param {CreateProjectReq} createProjectReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createProject: async (createProjectReq: CreateProjectReq, options: any = {}): Promise<RequestArgs> => {
            // verify required parameter 'createProjectReq' is not null or undefined
            assertParamExists('createProject', 'createProjectReq', createProjectReq)
            const localVarPath = `/project`;
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'POST', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            localVarHeaderParameter['Content-Type'] = 'application/json';

            setSearchParams(localVarUrlObj, localVarQueryParameter, options.query);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};
            localVarRequestOptions.data = serializeDataIfNeeded(createProjectReq, localVarRequestOptions, configuration)

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
    }
};

/**
 * ProjectApi - functional programming interface
 * @export
 */
export const ProjectApiFp = function(configuration?: Configuration) {
    const localVarAxiosParamCreator = ProjectApiAxiosParamCreator(configuration)
    return {
        /**
         * 
         * @summary Creates a new Xtages project
         * @param {CreateProjectReq} createProjectReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async createProject(createProjectReq: CreateProjectReq, options?: any): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Project>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.createProject(createProjectReq, options);
            return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration);
        },
    }
};

/**
 * ProjectApi - factory interface
 * @export
 */
export const ProjectApiFactory = function (configuration?: Configuration, basePath?: string, axios?: AxiosInstance) {
    const localVarFp = ProjectApiFp(configuration)
    return {
        /**
         * 
         * @summary Creates a new Xtages project
         * @param {CreateProjectReq} createProjectReq 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createProject(createProjectReq: CreateProjectReq, options?: any): AxiosPromise<Project> {
            return localVarFp.createProject(createProjectReq, options).then((request) => request(axios, basePath));
        },
    };
};

/**
 * ProjectApi - object-oriented interface
 * @export
 * @class ProjectApi
 * @extends {BaseAPI}
 */
export class ProjectApi extends BaseAPI {
    /**
     * 
     * @summary Creates a new Xtages project
     * @param {CreateProjectReq} createProjectReq 
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof ProjectApi
     */
    public createProject(createProjectReq: CreateProjectReq, options?: any) {
        return ProjectApiFp(this.configuration).createProject(createProjectReq, options).then((request) => request(this.axios, this.basePath));
    }
}


