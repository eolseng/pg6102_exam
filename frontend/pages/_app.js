import '../styles/globals.css'
import Layout from "../components/layout/Layout";
import {UserContextProvider} from "../contexts/UserContext";
import 'bootstrap/dist/css/bootstrap.min.css';
import UserInfo from "../components/layout/UserInfo";
import React from "react";

export default function MyApp({Component, pageProps}) {
    return (
        <UserContextProvider>
            <UserInfo/>
            <Layout>
                <Component {...pageProps} />
            </Layout>
        </UserContextProvider>
    )
}