import React, {useContext} from "react";
import {useRouter} from "next/router";
import Link from "next/link";

import Navbar from "react-bootstrap/Navbar";
import Nav from "react-bootstrap/Nav";
import Button from "react-bootstrap/Button";

import {UserContext} from "../../contexts/UserContext";
import {getUserData, logout} from "../../lib/auth";

export default function Header() {

    const {state} = useContext(UserContext)

    const greeting = () => {
        if (state.user) {
            return 'Signed in as ' + state.user.username + '!'
        } else {
            return 'Welcome!'
        }
    }

    const links = () => {
        // Map of links with [DISPLAY, HREF]
        const linkMap = new Map([
            ["Blueprints", "/blueprints"],
        ])

        const links = []
        linkMap.forEach((href, name) => {
            links.push(
                <Link key={href} href={href}>
                    <Nav.Link href={href}>{name}</Nav.Link>
                </Link>
            )
        })

        return links
    }

    const authButtons = () => {
        if (state.user) {
            return <LogoutButton/>
        } else {
            return (<>
                <Link href={"/signup"}>
                    <Button className={"m-1"}>Signup</Button>
                </Link>
                <Link href={"/login"}>
                    <Button className={"m-1"}>Login</Button>
                </Link>
            </>);
        }
    }

    return (
        <Navbar bg={"light"} className={"d-flex justify-content-between align-items-center border-bottom"}>
            <div className={"container"}>
                <Link href={"/"}>
                    <Navbar.Brand href={"/"}>LOGO</Navbar.Brand>
                </Link>
                <Navbar.Text className={"align-self-center"}>
                    {greeting()}
                </Navbar.Text>
                <Nav className="">
                    {links()}
                    {authButtons()}
                </Nav>
            </div>
        </Navbar>
    )
};

export function LogoutButton() {

    const {dispatch} = useContext(UserContext)
    const router = useRouter()

    const handleLogout = async () => {
        const result = await logout()
        if (result) {
            await getUserData()
            dispatch({type: "setUser"})
            await router.push("/")
        }
    }
    return <Button onClick={handleLogout} className={"m-1"}>Logout</Button>
}