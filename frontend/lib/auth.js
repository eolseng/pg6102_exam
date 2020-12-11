import axios from 'axios'

const BASE_PATH = "/api/v1/auth"

export async function getUserData() {
    return axios
        .get(BASE_PATH + "/user")
        .then(res => res.data)
        .catch(err => {
            console.error(err)
            return false
        })
}

/**
 * Signup a new user for the service
 *
 * @param username
 * @param password
 * @returns {Promise<boolean>} if user is registered or not
 */
export async function signUp(username, password) {
    return axios
        .put(BASE_PATH + "/signup/" + username, {
            username: username,
            password: password
        })
        .then(res => res.status === 201)
        .catch(() => false)
}

export async function login(username, password) {
    return axios
        .post(BASE_PATH + "/login", {
            username: username,
            password: password
        })
        .then(res => res.status === 204)
        .catch(() => false)
}

export async function logout() {
    return axios
        .post(BASE_PATH + "/logout")
        .then(res => res.status === 204)
        .catch(() => false)
}