import React from "react";
import Header from "./Header";

export default function Layout(props) {
    return (
        <div className={"container-fluid px-0 min-vh-100"}>
            <Header/>
            <main
                className={"container mh-100"}
            >
                {props.children}
            </main>
        </div>
    )
};