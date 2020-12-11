import React, {useContext, useState} from "react";
import {UserContext} from "../contexts/UserContext";
import {useRouter} from "next/router";
import {getUserData, login} from "../lib/auth";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";

export default function Login() {
    const {state, dispatch} = useContext(UserContext)
    const router = useRouter()

    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")
    const [errorMsg, setErrorMsg] = useState(null)

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (username.length < 3) {
            setErrorMsg("Username must be at least 3 characters")
        } else if (password.length < 3) {
            setErrorMsg("Password must be at least 3 characters")
        } else {
            const result = await login(username, password)
            console.log(result)
            if (result) {
                const user = await getUserData()
                dispatch({type: "setUser", payload: user.data})
                await router.push("/")
            } else {
                setErrorMsg("Login failed.")
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


    return (
        <Form
            onSubmit={handleSubmit}
            className={"border border-dark rounded my-4 mx-auto p-4 mw-80"}
            style={{width: "300px"}}
        >
            <h3 className={"text-center"}>Login</h3>
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
            {/* SUBMIT BUTTON */}
            <Button type={"submit"}>Login</Button>
            {errorMsg &&
            <div className={"m-2 p-2 border border-danger rounded"}>{errorMsg}</div>
            }
        </Form>
    );
}