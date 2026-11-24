import { useState } from 'react';
import { login, register } from './authService';
import Logo from './icons/Logo';
import './icons/icons.css';
import './Login.css';

function Login({ onLogin }) {

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isRegistering, setIsRegistering] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    async function handleSubmit(event) {
        event.preventDefault();

        setError('');
        setSuccess('');
        setIsLoading(true);

        try {
            if (isRegistering) {
                await register(email, password);

                setSuccess('Please check your email to confirm your account.');
                setIsRegistering(false);
                setPassword('');
            } else {
                const data = await login(email, password);

                if (!data.authenticated) {
                    throw new Error('Login failed.');
                }

                onLogin(true);
            }
        } catch (error) {
            setError(error.message);
        } finally {
            setIsLoading(false);
        }
    }

    function toggleMode() {
        setIsRegistering(previous => !previous);
        setError('');
        setSuccess('');
        setPassword('');
    }

    return (
        <div className="login-page">
            <div className="login-card">

                <div className="login-logo">
                    <Logo />
                </div>

                <h2>
                    {isRegistering
                        ? 'Create your account'
                        : 'Welcome back!'}
                </h2>

                <p className="login-subtitle">
                    Your personal nutrition assistant
                </p>

                <form onSubmit={handleSubmit}>

                    <label>Email</label>
                    <input
                        type="email"
                        placeholder="Enter your email"
                        value={email}
                        onChange={event => setEmail(event.target.value)}
                        required
                    />

                    <label>Password</label>
                    <input
                        type="password"
                        placeholder="Enter your password"
                        value={password}
                        onChange={event => setPassword(event.target.value)}
                        autoComplete={isRegistering
                            ? 'new-password'
                            : 'current-password'}
                        minLength={isRegistering ? 8 : undefined}
                        required
                    />

                    {error && (
                        <p className="login-error" role="alert">{error}</p>
                    )}

                    {success && (
                        <p className="login-success" role="status">{success}</p>
                    )}

                    <button
                        type="submit"
                        className="login-submit"
                        disabled={isLoading}
                    >
                        {isLoading
                            ? 'Please wait...'
                            : isRegistering
                                ? 'Create account'
                                : 'Log in'}
                    </button>

                </form>

                <div className="login-switch">
                    {isRegistering
                        ? 'Already have an account?'
                        : "Don't have an account?"}

                    <button
                        type="button"
                        onClick={toggleMode}
                        disabled={isLoading}
                    >
                        {isRegistering ? 'Log in' : 'Sign up'}
                    </button>
                </div>

            </div>
        </div>
    );
}

export default Login;