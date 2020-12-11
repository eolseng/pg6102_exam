import {createContext, useReducer} from "react";

const initialState = {
    user: null
}
const UserContext = createContext(initialState);

const reducer = (state, action) => {
    switch (action.type) {
        case "setUser":
            if (action.payload) {
                return {...state, user: action.payload}
            } else {
                return initialState
            }
        default:
            return
    }
}

function UserContextProvider(props) {

    const [state, dispatch] = useReducer(reducer, initialState)
    const value = {state, dispatch}

    return (
        <UserContext.Provider value={value}>
            {props.children}
        </UserContext.Provider>
    )
}

export {UserContext, UserContextProvider}