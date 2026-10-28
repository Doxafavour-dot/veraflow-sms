import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const _ch = MethodChannel('fwd');

void main() => runApp(const ForwarderApp());

class ForwarderApp extends StatelessWidget {
  const ForwarderApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'SMS Forwarder',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(colorSchemeSeed: const Color(0xFF0F766E), useMaterial3: true),
        darkTheme: ThemeData(colorSchemeSeed: const Color(0xFF2DD4BF), brightness: Brightness.dark, useMaterial3: true),
        home: const Home(),
      );
}

class Home extends StatefulWidget {
  const Home({super.key});
  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> with WidgetsBindingObserver {
  final _to = TextEditingController();
  final _user = TextEditingController();
  final _pass = TextEditingController();
  final _senders = TextEditingController();
  final _skip = TextEditingController();
  bool _needAmount = true;
  bool _enabled = true, _perm = false, _hasPass = false, _busy = false, _showPass = false;
  int _queue = 0;
  List<dynamic> _log = [];
  String _msg = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState s) {
    if (s == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _load() async {
    final m = await _ch.invokeMapMethod<String, dynamic>('getState') ?? {};
    setState(() {
      _to.text = (m['to'] ?? '') as String;
      _user.text = (m['user'] ?? '') as String;
      _senders.text = (m['senders'] ?? '') as String;
      _skip.text = (m['skip'] ?? '') as String;
      _needAmount = (m['needAmount'] ?? true) as bool;
      _enabled = (m['enabled'] ?? true) as bool;
      _hasPass = (m['hasPass'] ?? false) as bool;
      _perm = (m['perm'] ?? false) as bool;
      _queue = (m['queue'] ?? 0) as int;
      _log = jsonDecode((m['log'] ?? '[]') as String) as List<dynamic>;
    });
  }

  Future<void> _refresh() async {
    final m = await _ch.invokeMapMethod<String, dynamic>('getState') ?? {};
    if (!mounted) return;
    setState(() {
      _perm = (m['perm'] ?? false) as bool;
      _queue = (m['queue'] ?? 0) as int;
      _log = jsonDecode((m['log'] ?? '[]') as String) as List<dynamic>;
    });
  }

  Future<void> _save() async {
    await _ch.invokeMethod('save', {
      'to': _to.text,
      'user': _user.text,
      'pass': _pass.text,
      'senders': _senders.text,
      'skip': _skip.text,
      'needAmount': _needAmount,
      'enabled': _enabled,
    });
    _pass.clear();
    await _load();
    setState(() => _msg = 'Saved.');
  }

  Future<void> _test() async {
    setState(() { _busy = true; _msg = 'Sending a test email...'; });
    await _save();
    final r = await _ch.invokeMethod<String>('test');
    await _refresh();
    setState(() { _busy = false; _msg = r ?? ''; });
  }

  String _when(int t) {
    final d = DateTime.fromMillisecondsSinceEpoch(t);
    String two(int n) => n.toString().padLeft(2, '0');
    return '${d.year}-${two(d.month)}-${two(d.day)} ${two(d.hour)}:${two(d.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('SMS Forwarder')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            color: _perm ? cs.primaryContainer : cs.errorContainer,
            child: ListTile(
              leading: Icon(_perm ? Icons.check_circle : Icons.warning_amber),
              title: Text(_perm ? 'Allowed to receive SMS' : 'Needs permission to receive SMS'),
              subtitle: Text(_perm ? 'Waiting to send: $_queue' : 'Tap Allow, then choose Allow on the prompt.'),
              trailing: _perm ? null : FilledButton(onPressed: () async { await _ch.invokeMethod('requestPermission'); await Future.delayed(const Duration(seconds: 2)); _refresh(); }, child: const Text('Allow')),
            ),
          ),
          SwitchListTile(title: const Text('Forwarding is on'), value: _enabled, onChanged: (v) => setState(() => _enabled = v)),
          const SizedBox(height: 8),
          TextField(controller: _senders, minLines: 1, maxLines: 3, decoration: const InputDecoration(labelText: 'Senders to forward', helperText: 'Type each sender exactly as Messages shows it, separated by commas. Nothing else is forwarded.', border: OutlineInputBorder())),
          const SizedBox(height: 12),
          TextField(controller: _skip, minLines: 1, maxLines: 3, decoration: const InputDecoration(labelText: 'Skip messages containing', helperText: 'Messages with any of these words or phrases are not forwarded. Separate with commas.', border: OutlineInputBorder())),
          SwitchListTile(contentPadding: EdgeInsets.zero, title: const Text('Only forward messages with an amount of money'), subtitle: const Text('Skips promos, news and codes that mention no amount.'), value: _needAmount, onChanged: (v) => setState(() => _needAmount = v)),
          const SizedBox(height: 12),
          TextField(controller: _to, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Send to (your email)', border: OutlineInputBorder())),
          const SizedBox(height: 12),
          TextField(controller: _user, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Your Gmail address (sends the mail)', border: OutlineInputBorder())),
          const SizedBox(height: 12),
          TextField(
            controller: _pass,
            obscureText: !_showPass,
            decoration: InputDecoration(
              labelText: _hasPass ? 'App password (saved, leave blank to keep)' : 'Gmail app password',
              helperText: 'A 16-letter app password from your Google account, not your real password.',
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(icon: Icon(_showPass ? Icons.visibility_off : Icons.visibility), onPressed: () => setState(() => _showPass = !_showPass)),
            ),
          ),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(child: FilledButton(onPressed: _busy ? null : _save, child: const Text('Save'))),
            const SizedBox(width: 12),
            Expanded(child: OutlinedButton(onPressed: _busy ? null : _test, child: const Text('Send test email'))),
          ]),
          if (_msg.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 12), child: Text(_msg)),
          if (_queue > 0)
            Padding(padding: const EdgeInsets.only(top: 8), child: TextButton(onPressed: () async { await _ch.invokeMethod('flush'); setState(() => _msg = 'Trying to send the waiting messages.'); }, child: Text('Send $_queue waiting now'))),
          const SizedBox(height: 20),
          Row(children: [
            Text('Recent activity', style: Theme.of(context).textTheme.titleMedium),
            const Spacer(),
            TextButton(onPressed: () async { await _ch.invokeMethod('clearLog'); _refresh(); }, child: const Text('Clear')),
          ]),
          const Text('The log shows only the sender and the result, never the message.', style: TextStyle(fontSize: 12)),
          const SizedBox(height: 4),
          if (_log.isEmpty) const Padding(padding: EdgeInsets.all(12), child: Text('Nothing yet.')),
          for (final e in _log)
            ListTile(
              dense: true,
              leading: Icon((e['ok'] as bool) ? Icons.check : Icons.error_outline, color: (e['ok'] as bool) ? cs.primary : cs.error),
              title: Text('${e['s']}'),
              subtitle: Text('${_when(e['t'] as int)}  ${e['n']}'),
            ),
          const SizedBox(height: 24),
          const Text('Tip: on some phones you must also turn off battery optimisation for this app (Settings, Apps, SMS Forwarder, Battery, Unrestricted), or Android may delay messages.', style: TextStyle(fontSize: 12)),
        ],
      ),
    );
  }
}
