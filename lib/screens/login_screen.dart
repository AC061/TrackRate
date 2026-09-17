import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../api/trackrate_client.dart';
import '../providers/trackrate_providers.dart';
import '../routing/app_routes.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key, this.redirectTo});

  final String? redirectTo;

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _identifierController = TextEditingController();
  final _passwordController = TextEditingController();
  final _emailController = TextEditingController();
  bool _registerMode = false;
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _identifierController.dispose();
    _passwordController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  void _navigateAfterAuth() {
    final redirect = widget.redirectTo;
    if (redirect != null && redirect.isNotEmpty && redirect.startsWith('/')) {
      context.go(redirect);
      return;
    }
    context.go(AppRoutes.home);
  }

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      if (_registerMode) {
        await ref.read(authSessionProvider.notifier).register(
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
      } else {
        await ref.read(authSessionProvider.notifier).login(
          identifier: _identifierController.text.trim(),
          password: _passwordController.text,
        );
      }

      if (!mounted) {
        return;
      }
      final auth = ref.read(authSessionProvider);
      if (auth.hasError) {
        setState(() {
          _error = auth.error is TrackRateException
              ? (auth.error as TrackRateException).message
              : 'Error de autenticación';
          _submitting = false;
        });
        return;
      }
      _navigateAfterAuth();
    } catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _error = e is TrackRateException ? e.message : 'Error inesperado';
        _submitting = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_registerMode ? 'Crear cuenta' : 'Iniciar sesión'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          if (_registerMode)
            TextField(
              controller: _emailController,
              decoration: const InputDecoration(
                labelText: 'Email',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.emailAddress,
              autocorrect: false,
            )
          else
            TextField(
              controller: _identifierController,
              decoration: const InputDecoration(
                labelText: 'Email o usuario',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.emailAddress,
              autocorrect: false,
            ),
          const SizedBox(height: 16),
          TextField(
            controller: _passwordController,
            decoration: const InputDecoration(
              labelText: 'Contraseña',
              border: OutlineInputBorder(),
            ),
            obscureText: true,
          ),
          if (_error != null) ...[
            const SizedBox(height: 16),
            Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: _submitting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(_registerMode ? 'Registrarse' : 'Entrar'),
          ),
          TextButton(
            onPressed: _submitting
                ? null
                : () => setState(() {
                    _registerMode = !_registerMode;
                    _error = null;
                  }),
            child: Text(
              _registerMode
                  ? '¿Ya tienes cuenta? Inicia sesión'
                  : '¿No tienes cuenta? Regístrate',
            ),
          ),
        ],
      ),
    );
  }
}
