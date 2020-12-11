import {useContext, useEffect} from "react";

import {UserContext} from "../../contexts/UserContext";
import {getUserData} from "../../lib/auth";

export default function UserInfo() {

    // Fetch initial user data
    const {state, dispatch} = useContext(UserContext)
    useEffect(() => {
        // Fetch user data and update user state
        getUserData().then(data => dispatch({type: "setUser", payload: data.data}))
    }, [])

    return null
}