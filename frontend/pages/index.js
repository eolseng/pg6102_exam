import styles from '../styles/Home.module.css'
import TripContainer from "../components/containers/tripContainer";

export default function Home() {
    return (
        <div className={styles.container + " mh-100"}>
            <h1>Amazing trips!</h1>
            <TripContainer/>
        </div>
    )
}
