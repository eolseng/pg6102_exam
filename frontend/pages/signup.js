import React, {useContext, useState} from "react";

import Form from "react-bootstrap/Form"
import Button from "react-bootstrap/Button";
import {getUserData, signUp} from "../lib/auth";
import {UserContext} from "../contexts/UserContext";
import {useRouter} from "next/router";

export default function SignUp() {

    const {state, dispatch} = useContext(UserContext)
    const router = useRouter()

    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [errorMsg, setErrorMsg] = useState(null)

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (username.length < 3) {
            setErrorMsg("Username must be at least 3 characters")
        } else if (password.length < 3) {
            setErrorMsg("Password must be at least 3 characters")
        } else if (password !== confirmPassword) {
            setErrorMsg("Passwords do not match")
        } else {
            const result = await signUp(username, password)
            if (result) {
                const user = await getUserData()
                dispatch({type: "setUser", payload: user.data})
                await router.push("/")
            }
        }
    }

    function usernameClass() {
        const length = username.length
        if (length > 0) {
            if (length < 3) {
                return "border border-warning"
            } else {
                return "border border-success"
            }
        }
    }

    function passClass() {
        const length = password.length
        if (length > 0) {
            if (length < 3) {
                return "border border-warning"
            } else {
                return "border border-success"
            }
        }
    }

    function confirmPassClass() {
        if (confirmPassword.length > 0 && password.length > 2) {
            if (confirmPassword === password) {
                return "border border-success"
            } else {
                return "border border-warning"
            }
        }
    }


    return (
        <Form
            onSubmit={handleSubmit}
            className={"border border-dark rounded my-4 mx-auto p-4 mw-80"}
            style={{width: "300px"}}
        >
            <h3 className={"text-center"}>Sign up</h3>
            {/* USERNAME */}
            <Form.Group controlId={"formUsername"}>
                <Form.Label>
                    Username:
                </Form.Label>
                <Form.Control
                    className={usernameClass()}
                    type={"text"}
                    placeholder={"Username"}
                    onChange={e => {
                        setUsername(e.target.value)
                        setErrorMsg(null)
                    }}
                />
            </Form.Group>
            {/* PASSWORD */}
            <Form.Group controlId={"formPassword"}>
                <Form.Label>
                    Password:
                </Form.Label>
                <Form.Control
                    className={passClass()}
                    type={"password"}
                    placeholder={"Password"}
                    onChange={e => {
                        setPassword(e.target.value)
                        setErrorMsg(null)
                    }}
                />
            </Form.Group>
            {/* CONFIRM PASSWORD */}
            <Form.Group controlId={"formConfirmPassword"}>
                <Form.Label>
                    Confirm password:
                </Form.Label>
                <Form.Control
                    className={confirmPassClass()}
                    type={"password"}
                    placeholder={"Confirm password"}
                    onChange={e => {
                        setConfirmPassword(e.target.value)
                        setErrorMsg(null)
                    }}
                />
            </Form.Group>
            {/* SUBMIT BUTTON */}
            <Button type={"submit"}>Sign up</Button>
            {errorMsg &&
            <div className={"m-2 p-2 border border-danger rounded"}>{errorMsg}</div>
            }
        </Form>
    );
}